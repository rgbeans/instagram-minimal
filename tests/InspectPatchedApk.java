import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Validate the delivered DEX files and the required injected hook families. */
public class InspectPatchedApk {
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> classes = new HashMap<>();
        int dexes=0, network=0, intents=0, fragments=0, captureRequests=0, captureReleases=0, chatCards=0;
        try (ZipFile zip=new ZipFile(args[0])) {
            for (ZipEntry entry:Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                dexes++;
                DexBackedDexFile dex=DexBackedDexFile.fromInputStream(Opcodes.getDefault(),new BufferedInputStream(zip.getInputStream(entry)));
                for (ClassDef cls:dex.getClasses()) if (classes.put(cls.getType(),cls)!=null) throw new AssertionError("Duplicate DEX class: "+cls.getType());
            }
            for (ClassDef cls:classes.values()) for (Method method:cls.getMethods()) {
                if (method.getImplementation()==null) continue;
                for (Instruction ins:method.getImplementation().getInstructions()) {
                    if (!(ins instanceof ReferenceInstruction)) continue;
                    Reference ref=((ReferenceInstruction)ins).getReference();
                    if (!(ref instanceof MethodReference)) continue;
                    MethodReference call=(MethodReference)ref;
                    if (!call.getDefiningClass().startsWith("Lcom/feurstagram/extension/")) continue;
                    if (cls.getType().startsWith("Lcom/feurstagram/")) continue;
                    ClassDef target=classes.get(call.getDefiningClass());
                    if (target==null) throw new AssertionError("Missing extension class: "+call);
                    boolean found=false;
                    for (Method candidate:target.getMethods()) if (candidate.getName().equals(call.getName()) && candidate.getParameterTypes().equals(call.getParameterTypes()) && candidate.getReturnType().equals(call.getReturnType())) {found=true;break;}
                    if (!found) throw new AssertionError("Missing extension method: "+call);
                    if (cls.getType().startsWith("Lcom/feurstagram/")) continue;
                    if (call.getDefiningClass().endsWith("/Block;") && call.getName().equals("throwIfBlocked")) network++;
                    if (call.getDefiningClass().endsWith("/NavigationGuard;") && call.getName().equals("filterIntent")) intents++;
                    if (call.getDefiningClass().endsWith("/ContentGuard;") && call.getName().equals("onFragmentViewCreated")) fragments++;
                    if (call.getDefiningClass().endsWith("/ScreenCapture;") && call.getName().equals("skipRequest")) captureRequests++;
                    if (call.getDefiningClass().endsWith("/ScreenCapture;") && call.getName().equals("skipRelease")) captureReleases++;
                    if (call.getDefiningClass().endsWith("/SharedMessages;") && call.getName().equals("onBind")) chatCards++;
                }
            }
        }
        if (network!=1 || intents<1 || fragments!=1) throw new AssertionError("Unexpected hooks: "+network+","+intents+","+fragments);
        if (captureRequests!=1 || captureReleases!=1 || chatCards<2) throw new AssertionError("Unexpected chat hooks: "+captureRequests+","+captureReleases+","+chatCards);
        verifyNativeNotifications(classes);
        System.out.println("PASS APK DEX structure: "+dexes+" DEX files, "+classes.size()+" unique classes, all injected extension calls resolve; hooks network="+network+", intents="+intents+", fragments="+fragments+", capture="+(captureRequests+captureReleases)+", chat cards="+chatCards);
    }

    private static void verifyNativeNotifications(Map<String, ClassDef> classes) {
        String extension = "Lcom/feurstagram/extension/";
        String chat = extension + "ChatControls;";
        String launcher = chat + "->openNotifications(Landroid/content/Context;)V";
        String navigator = "LX/08Sa;";
        String fragment = "Lcom/instagram/newsfeed/fragment/NewsfeedFragment;";
        List<String> transaction = List.of(
                navigator + "-><init>(Landroidx/fragment/app/FragmentActivity;LX/01yx;)V",
                "LX/01ql;->A01(I)Ljava/lang/Object;",
                fragment + "->A02(ZZ)" + fragment,
                navigator + "->A0F(Landroidx/fragment/app/Fragment;)V",
                navigator + "->A0A()V",
                navigator + "->A07()V");
        List<String> bridge = new ArrayList<>();
        bridge.add("LX/06Yx;->getSession()LX/01yx;");
        bridge.addAll(transaction);
        String bridgeSignature = chat + "->openNativeNotifications(Landroid/app/Activity;Ljava/lang/Object;)Z";
        if (!bridge.equals(calls(method(classes, bridgeSignature)))) {
            throw new AssertionError("Notifications bridge must open the native fragment with the current session");
        }
        for (String target : bridge) method(classes, target);
        List<String> nativeCalls = calls(method(classes, "LX/00I4;->A00(Landroidx/fragment/app/FragmentActivity;Lcom/instagram/common/session/UserSession;)V"));
        if (Collections.indexOfSubList(nativeCalls, transaction) == -1) {
            throw new AssertionError("Native action bar Notifications transaction changed");
        }
        for (String owner : List.of(extension + "ChatControls$Controller;", extension + "Settings;")) {
            boolean usesLauncher = false;
            for (Method m : classes.get(owner).getMethods()) {
                List<String> refs = calls(m);
                if (refs.contains(launcher)) usesLauncher = true;
                if (refs.contains("instagram://news")) {
                    throw new AssertionError("Legacy Notifications link remains in " + owner);
                }
            }
            if (!usesLauncher) throw new AssertionError("Missing Notifications shortcut in " + owner);
        }
        System.out.println("PASS native Notifications transaction and both entry points");
    }

    private static Method method(Map<String, ClassDef> classes, String descriptor) {
        ClassDef owner = classes.get(descriptor.substring(0, descriptor.indexOf("->")));
        if (owner != null) for (Method candidate : owner.getMethods()) {
            if (descriptor.equals(candidate.toString())) return candidate;
        }
        throw new AssertionError("Missing native Notifications method: " + descriptor);
    }

    private static List<String> calls(Method method) {
        List<String> refs = new ArrayList<>();
        if (method.getImplementation() == null) return refs;
        for (Instruction ins : method.getImplementation().getInstructions()) {
            if (ins instanceof ReferenceInstruction reference) {
                if (reference.getReference() instanceof MethodReference call) refs.add(call.toString());
                if (reference.getReference() instanceof StringReference string && "instagram://news".equals(string.getString())) {
                    refs.add(string.getString());
                }
            }
        }
        return refs;
    }
}
