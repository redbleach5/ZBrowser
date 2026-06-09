#!/bin/bash
set -e

# =============================================
# ZBrowser Full Build Script v2
# Builds APK with v1+v2+v3 signing for maximum compatibility
# Uses targetSdkVersion 28 for v1+v2+v3 compatibility
# =============================================

PROJECT=/home/z/my-project/ZBrowser
ANDROID_HOME=/home/z/my-project/android-sdk
JDK_HOME=/home/z/my-project/jdk/jdk-21.0.11+10
BUILD_TOOLS=$ANDROID_HOME/build-tools/35.0.1
PLATFORM=$ANDROID_HOME/platforms/android-34/android.jar

export JAVA_HOME=$JDK_HOME
export PATH=$JDK_HOME/bin:$BUILD_TOOLS:$PATH

APP_DIR=$PROJECT/app
BUILD_DIR=$PROJECT/app/build
COMPILED_RES=$BUILD_DIR/compiled_res
GEN_DIR=$BUILD_DIR/gen
OBJ_DIR=$BUILD_DIR/obj
DEX_DIR=$BUILD_DIR/dex
APK_DIR=$BUILD_DIR/apk
DIST_DIR=/home/z/my-project/download

# Clean previous build
rm -rf $BUILD_DIR
mkdir -p $COMPILED_RES $GEN_DIR $OBJ_DIR $DEX_DIR $APK_DIR $DIST_DIR

echo "=== Step 1: Generate keystore ==="
KEYSTORE=$BUILD_DIR/zbrowser.keystore
keytool -genkeypair \
    -v \
    -keystore $KEYSTORE \
    -alias zbrowser \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass zbrowser123 \
    -keypass zbrowser123 \
    -dname "CN=ZBrowser, O=ZBrowser, C=RU"

echo "=== Step 2: Compile resources with aapt2 ==="
aapt2 compile --dir $APP_DIR/src/main/res -o $COMPILED_RES/resources.zip

echo "=== Step 3: Link resources ==="
aapt2 link \
    -o $APK_DIR/app.apk \
    --manifest $APP_DIR/src/main/AndroidManifest.xml \
    -I $PLATFORM \
    --java $GEN_DIR \
    --target-sdk-version 28 \
    --min-sdk-version 21 \
    --version-code 3 \
    --version-name 2.0.0 \
    --auto-add-overlay \
    $COMPILED_RES/resources.zip

echo "=== Step 4: Compile Java sources ==="
JAVA_SOURCES=$(find $APP_DIR/src/main/java -name "*.java")
GEN_SOURCES=$(find $GEN_DIR -name "*.java")

javac \
    -source 11 -target 11 \
    -classpath $PLATFORM \
    -d $OBJ_DIR \
    $JAVA_SOURCES $GEN_SOURCES

echo "=== Step 5: Convert to DEX ==="
d8 \
    --lib $PLATFORM \
    --output $DEX_DIR \
    $(find $OBJ_DIR -name "*.class")

echo "=== Step 6: Build unsigned APK ==="
cd $APK_DIR
cp app.apk unsigned.apk
zip -j unsigned.apk $DEX_DIR/classes.dex

echo "=== Step 7: Zipalign ==="
zipalign -f 4 unsigned.apk aligned.apk

echo "=== Step 8: Sign APK ==="
# Sign with apksigner v1+v2+v3
# With targetSdkVersion 28, v1+v2 should work together
apksigner sign \
    --ks $KEYSTORE \
    --ks-key-alias zbrowser \
    --ks-pass pass:zbrowser123 \
    --key-pass pass:zbrowser123 \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --v1-signer-name ZBROWSER \
    --out $DIST_DIR/ZBrowser.apk \
    aligned.apk

echo "=== Step 9: Verify signing ==="
apksigner verify --verbose $DIST_DIR/ZBrowser.apk

echo "=== Step 10: Verify APK info ==="
aapt dump badging $DIST_DIR/ZBrowser.apk 2>&1 | head -5

echo ""
echo "=== BUILD COMPLETE ==="
ls -la $DIST_DIR/ZBrowser.apk
