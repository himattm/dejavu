#!/usr/bin/env bash
#
# patch-jetchat.sh — Patch a Jetchat checkout to use Dejavu from mavenLocal.
#
# Usage: ./patch-jetchat.sh <path-to-jetchat>
#
# Idempotent: safe to run multiple times without duplicating entries.

set -euo pipefail

JETCHAT_DIR="${1:?Usage: $0 <path-to-jetchat>}"

if [ ! -d "$JETCHAT_DIR" ]; then
  echo "ERROR: Directory not found: $JETCHAT_DIR"
  exit 1
fi

SETTINGS_FILE="$JETCHAT_DIR/settings.gradle.kts"
APP_BUILD_FILE="$JETCHAT_DIR/app/build.gradle.kts"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_SRC="$SCRIPT_DIR/ConversationRecompositionTest.kt"

# --- 1. Add mavenLocal() to settings.gradle.kts repositories ---

if [ ! -f "$SETTINGS_FILE" ]; then
  echo "ERROR: settings.gradle.kts not found at $SETTINGS_FILE"
  exit 1
fi

# Verify mavenLocal() is present; if not, add it as the first entry inside the
# dependencyResolutionManagement repositories block.
if ! grep -q 'mavenLocal()' "$SETTINGS_FILE"; then
  echo "Adding mavenLocal() to $SETTINGS_FILE"
  # Find "repositories {" inside dependencyResolutionManagement and insert mavenLocal() after it.
  # Use awk to insert after the SECOND "repositories {" (the one inside dependencyResolutionManagement).
  awk '/repositories \{/{count++} count==2 && /repositories \{/{print; print "        mavenLocal()"; count++; next} 1' \
    "$SETTINGS_FILE" > "$SETTINGS_FILE.tmp"
  mv "$SETTINGS_FILE.tmp" "$SETTINGS_FILE"
else
  echo "mavenLocal() already present in $SETTINGS_FILE"
fi

# --- 2. Add Dejavu dependencies to app/build.gradle.kts ---

if [ ! -f "$APP_BUILD_FILE" ]; then
  echo "ERROR: app/build.gradle.kts not found at $APP_BUILD_FILE"
  exit 1
fi

DEJAVU_DEP='androidTestImplementation("me.mmckenna.dejavu:dejavu:0.1.1")'

if ! grep -q 'me.mmckenna.dejavu' "$APP_BUILD_FILE"; then
  echo "Adding Dejavu dependency to $APP_BUILD_FILE"
  # Insert before the closing brace of the dependencies block (last '}' in file)
  # We find the last androidTestImplementation line and append after it
  sed -i.bak "/androidTestImplementation(libs.androidx.compose.ui.test.junit4)/a\\
    $DEJAVU_DEP" "$APP_BUILD_FILE"
  rm -f "$APP_BUILD_FILE.bak"
else
  echo "Dejavu dependency already present in $APP_BUILD_FILE"
fi

# --- 3. Copy the test file into Jetchat ---

TEST_DEST_DIR="$JETCHAT_DIR/app/src/androidTest/java/com/example/compose/jetchat"
mkdir -p "$TEST_DEST_DIR"
cp "$TEST_SRC" "$TEST_DEST_DIR/ConversationRecompositionTest.kt"
echo "Copied test file to $TEST_DEST_DIR/ConversationRecompositionTest.kt"

echo "Done. Jetchat is patched for Dejavu integration tests."
