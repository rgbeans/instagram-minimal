"""Verify signed-out navigation, recording emulator ANRs rather than hiding them."""
import argparse
import json
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--package', required=True)
parser.add_argument('--output', required=True, type=pathlib.Path)
parser.add_argument('--prefix', default='FULL_APP_STARTUP_RESULT')
parser.add_argument('--relaunch-component')
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
deadline = time.monotonic() + 180
relaunch_after = time.monotonic() + 30
result = {'package': args.package, 'passed': False, 'anr_wait_recoveries': 0,
          'startup_ui_verified': False, 'login_form_visible': False, 'startup_relaunches': 0,
          'signed_in_features_tested': False}
observed_pids = set()
LOGIN_ROUTES = ['i already have a profile', 'i already have an account', 'log in to another account',
                'log into another account', 'log in', 'log into existing account']


def adb(*words, timeout=25):
    return subprocess.run(['adb', *words], capture_output=True, timeout=timeout, check=True).stdout


def screenshot(name):
    try:
        (args.output / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    except subprocess.SubprocessError:
        pass


def words(node):
    return ' '.join((node.get('text', ''), node.get('content-desc', ''))).strip()


def tap(node):
    coords = [int(value) for value in re.findall(r'\d+', node.get('bounds', ''))]
    if len(coords) != 4:
        raise AssertionError('The selected UI control has no valid bounds')
    adb('shell', 'input', 'tap', str((coords[0] + coords[2]) // 2), str((coords[1] + coords[3]) // 2))


def snapshot(name):
    try:
        adb('shell', 'uiautomator', 'dump', '/sdcard/minimal-probe.xml')
        raw = adb('shell', 'cat', '/sdcard/minimal-probe.xml')
        root = ET.fromstring(raw)
        (args.output / (name + '.xml')).write_bytes(raw)
        return list(root.iter('node'))
    except (subprocess.SubprocessError, ET.ParseError) as error:
        print('UI_PROBE_NOT_READY=' + str(type(error).__name__), flush=True)
        return []


def wait_for_page(name):
    missing_process = 0
    while time.monotonic() < deadline:
        pid = subprocess.run(['adb', 'shell', 'pidof', args.package], capture_output=True, timeout=15).stdout.decode().strip()
        observed_pids.update(pid.split())
        missing_process = missing_process + 1 if not pid else 0
        if missing_process >= 2:
            raise AssertionError('The app process exited during startup')
        nodes = snapshot(name)
        system = [node for node in nodes if node.get('package') == 'android']
        anr = any("isn't responding" in words(node).lower() or 'not responding' in words(node).lower()
                  for node in system)
        wait = next((node for node in system if words(node).strip().lower() == 'wait'), None)
        if anr and wait is not None:
            result['anr_wait_recoveries'] += 1
            screenshot('cold-start-anr-' + str(result['anr_wait_recoveries']))
            assert result['anr_wait_recoveries'] <= 2, 'The app remains unresponsive after selecting Wait twice'
            print('UI_PROBE_ANR_WAIT=' + str(result['anr_wait_recoveries']), flush=True)
            tap(wait)
        else:
            app_nodes = [node for node in nodes if node.get('package') == args.package]
            ready = has_password(app_nodes) or any(words(node).lower() in LOGIN_ROUTES for node in app_nodes)
            print('UI_PROBE_STATE=' + json.dumps({'app_nodes': len(app_nodes), 'ready': ready,
                  'text': [words(node) for node in nodes if words(node)][:8]}), flush=True)
            if app_nodes and ready:
                screenshot(name)
                return app_nodes
            if (name == 'startup' and args.relaunch_component and not app_nodes
                    and not result['startup_relaunches'] and time.monotonic() >= relaunch_after
                    and any(node.get('package') in {'com.android.launcher3', 'com.google.android.apps.nexuslauncher'}
                            for node in nodes)):
                result['startup_relaunches'] = 1
                screenshot('launcher-before-retry')
                print('UI_PROBE_RELAUNCH=1', flush=True)
                output = adb('shell', 'am', 'start', '-W', '-a', 'android.intent.action.MAIN',
                             '-c', 'android.intent.category.LAUNCHER', '-f', '0x10200000',
                             '-n', args.relaunch_component, timeout=45)
                (args.output / 'relaunch.txt').write_bytes(output)
        time.sleep(8)
    raise AssertionError('No usable app UI appeared within the startup time limit')


def has_password(nodes):
    return any(node.get('password') == 'true' for node in nodes) or (
        any('password' in words(node).lower() for node in nodes)
        and any('EditText' in node.get('class', '') for node in nodes))


failure = None
try:
    nodes = wait_for_page('startup')
    result['startup_ui_verified'] = True
    result['startup_ui_nodes'] = len(nodes)
    result['startup_visible_text'] = sorted({words(node) for node in nodes if words(node)})
    for step in range(3):
        if has_password(nodes):
            break
        target = next((node for label in LOGIN_ROUTES for node in nodes if words(node).lower() == label), None)
        assert target is not None, 'The app UI appeared, but no existing-account login route was found'
        tap(target)
        time.sleep(8)
        nodes = wait_for_page('login')
    assert has_password(nodes), 'Navigation did not reach a visible password input'
    result['login_form_visible'] = True
    result['login_visible_text'] = sorted({words(node) for node in nodes if words(node)})
    screenshot('login')
    assert subprocess.run(['adb', 'shell', 'pidof', args.package], capture_output=True, timeout=15).stdout.strip(), 'The app exited after login navigation'
    result['passed'] = True
except (AssertionError, subprocess.SubprocessError) as error:
    failure = error
    result['error'] = str(error)
    screenshot('failure')
finally:
    log = subprocess.run(['adb', 'logcat', '-d', '-t', '10000'], capture_output=True, timeout=30).stdout.decode(errors='replace')
    (args.output / 'logcat.txt').write_text(log)
    crashes = adb('logcat', '-b', 'crash', '-d', timeout=20).decode(errors='replace')
    (args.output / 'crash-logcat.txt').write_text(crashes)
    combined_log = log + '\n' + crashes
    lines = combined_log.splitlines()
    app_crash_pids = set(re.findall(r'pid: (\d+).*>>> ' + re.escape(args.package) + r' <<<', combined_log))
    native = [line for line in lines if 'Fatal signal' in line
              and any(re.search(r'\bpid ' + re.escape(pid) + r'\b', line) for pid in observed_pids | app_crash_pids)]
    result['native_crash_lines'] = native
    app_java_crashes = [line for line in crashes.splitlines()
                        if re.search(r'Process: ' + re.escape(args.package) + r'(?:,|:)', line)]
    result['java_crash_lines'] = app_java_crashes
    if native:
        result['passed'] = False
        result['error'] = 'Native app crash was captured during the probe'
    elif app_java_crashes:
        result['passed'] = False
        result['error'] = 'Java app crash was captured during the probe'
    selected = [line for line in lines if 'Fatal signal' in line or 'F DEBUG' in line or 'AndroidRuntime' in line]
    for line in selected[-100:]:
        print(line[:2000])
    (args.output / 'startup-result.json').write_text(json.dumps(result, indent=2) + '\n')
    print(args.prefix + '=' + json.dumps(result), flush=True)
    if result['anr_wait_recoveries']:
        print('::warning::Cold startup produced ANR dialogs; Wait was selected and the result records whether login navigation recovered.')
if not result['passed']:
    raise SystemExit(1)
