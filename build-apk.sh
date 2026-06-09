#!/bin/bash
set -e

# =============================================
# ZBrowser Full Build Script v3 — GeckoView
# Builds APK with v1+v2+v3 signing
# Includes Mozilla GeckoView engine (independent of system WebView)
# =============================================

PROJECT=/home/z/my-project/ZBrowser
ANDROID_HOME=/home/z/my-project/android-sdk
JDK_HOME=/home/z/my-project/jdk/jdk-21.0.11+10
BUILD_TOOLS=$ANDROID_HOME/build-tools/35.0.1
PLATFORM=$ANDROID_HOME/platforms/android-34/android.jar

# GeckoView extracted AAR location
GV_EXTRACTED=$PROJECT/geckoview/extracted
GV_CLASSES_JAR=$GV_EXTRACTED/classes.jar

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

# Also compile GeckoView resources if they exist
if [ -d "$GV_EXTRACTED/res" ]; then
    echo "  Compiling GeckoView resources..."
    aapt2 compile --dir $GV_EXTRACTED/res -o $COMPILED_RES/geckoview_res.zip
    GV_RES_ARG="$COMPILED_RES/geckoview_res.zip"
else
    GV_RES_ARG=""
fi

echo "=== Step 3: Link resources ==="
aapt2 link \
    -o $APK_DIR/app.apk \
    --manifest $APP_DIR/src/main/AndroidManifest.xml \
    -I $PLATFORM \
    --java $GEN_DIR \
    --target-sdk-version 28 \
    --min-sdk-version 21 \
    --version-code 4 \
    --version-name 3.0.0 \
    --auto-add-overlay \
    $COMPILED_RES/resources.zip \
    $GV_RES_ARG

echo "=== Step 4: Compile Java sources ==="
JAVA_SOURCES=$(find $APP_DIR/src/main/java -name "*.java")
GEN_SOURCES=$(find $GEN_DIR -name "*.java")

javac \
    -source 11 -target 11 \
    -classpath "$PLATFORM:$GV_CLASSES_JAR" \
    -d $OBJ_DIR \
    $JAVA_SOURCES $GEN_SOURCES

echo "=== Step 5: Convert to DEX ==="
# First d8 our app classes
d8 \
    --lib $PLATFORM \
    --output $DEX_DIR \
    $(find $OBJ_DIR -name "*.class")

# Then d8 GeckoView classes.jar into separate dex files
echo "  Converting GeckoView classes to DEX..."
d8 \
    --lib $PLATFORM \
    --output $DEX_DIR \
    --min-api 21 \
    $GV_CLASSES_JAR

echo "=== Step 6: Build unsigned APK ==="
cd $APK_DIR
cp app.apk unsigned.apk

# Add all DEX files
for dex in $DEX_DIR/classes*.dex; do
    echo "  Adding $(basename $dex)..."
    zip -j unsigned.apk "$dex"
done

# Add GeckoView native libraries using Python for correct APK paths
echo "  Adding GeckoView native libraries..."
python3 << 'PYEOF'
import zipfile, os, shutil

apk_path = os.environ.get('APK_DIR', '/home/z/my-project/ZBrowser/app/build/apk') + '/unsigned.apk'
gv_jni = '/home/z/my-project/ZBrowser/geckoview/extracted/jni'
gv_assets = '/home/z/my-project/ZBrowser/geckoview/extracted/assets'

with zipfile.ZipFile(apk_path, 'a', zipfile.ZIP_DEFLATED) as zf:
    # Add native libraries - arm64-v8a only (for modern devices)
    arch_dir = os.path.join(gv_jni, 'arm64-v8a')
    if os.path.isdir(arch_dir):
        for so in os.listdir(arch_dir):
            if so.endswith('.so'):
                so_path = os.path.join(arch_dir, so)
                arcname = f'lib/arm64-v8a/{so}'
                print(f'  Adding {arcname} ({os.path.getsize(so_path)//1024//1024} MB)')
                zf.write(so_path, arcname, compress_type=zipfile.ZIP_STORED)
    
    # Add assets (omni.ja etc.)
    if os.path.isdir(gv_assets):
        for root, dirs, files in os.walk(gv_assets):
            for f in files:
                full = os.path.join(root, f)
                rel = os.path.relpath(full, gv_assets)
                arcname = f'assets/{rel}'
                print(f'  Adding {arcname}')
                zf.write(full, arcname, compress_type=zipfile.ZIP_DEFLATED)

print('Done adding GeckoView files')
PYEOF

echo "=== Step 7: Zipalign ==="
cd $APK_DIR
zipalign -f 4 unsigned.apk aligned.apk

echo "=== Step 8: Sign APK ==="
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
ls -lh $DIST_DIR/ZBrowser.apk
