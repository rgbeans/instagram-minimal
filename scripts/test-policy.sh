#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
mkdir -p "$ROOT/build/tests"
"$JAVAC" -d "$ROOT/build/tests" "$ROOT/extensions/extension/src/main/java/com/feurstagram/extension/MinimalPolicy.java" "$ROOT/tests/MinimalPolicyTest.java"
"$JAVA" -cp "$ROOT/build/tests" MinimalPolicyTest
