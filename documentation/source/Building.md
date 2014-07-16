Building LiquidFun Paint and Running
=======================================

LiquidFun Paint is powered by LiquidFun, an open source technology available at
http://google.github.io/liquidfun/.

# Building for Android

### Version Requirements

Following are the minimum required versions for the tools and libraries you
need for building LiquidFun for Android:

-   Android SDK:  Android 2.3 (API Level 9)
-   ADT: 22.6.2
-   NDK: android-ndk-r9
-   NDK plugin for Eclipse: bundled with ADT
-   SWIG: 2.0.11

### Before Building

-   Install the [Android SDK].
-   Install the [Android NDK].
-   Install [Apache Ant].
-   Install [SWIG].

### Building

LiquidFun Paint uses [Android NDK] to build the native C/C++ component, and
[Apache Ant] or Eclipse to build the Java component.
It has an associated `AndroidManifest.xml` file, `build.xml` file and `jni`
subdirectory.  `AndroidManifest.xml` and `build.xml` contain details on how to
build an Android package (apk).  The `AndroidManifest.xml` file also informs the
`ndk-build` tool that the `jni` subdirectory contains NDK makefiles.

For convenience, a build script has been included with the distribution to aid
with commandline building.

To build on the commandline:

-   Open a command line window.
-   Go to the project directory.
-   Execute build script.

For example, to build the LiquidFun Paint unsigned apk:

    cd liquidfunpaint/
    ./AutoBuild/build_android.py

You can then sign the apk with an appropriate key and dispatch it onto a device.

### Before running

-   Install [ADT].
-   Install the [NDK Eclipse plugin].

#### Running an application using Eclipse:

-   Open [ADT][] Eclipse.
-   Select "File->Import..." from the menu.
-   Select "Android > Existing Android Code Into Workspace", and click "Next".
-   Click the "Browse..." button next to `Root Directory:` and select the
    project folder (e.g. `liquidfunpaint/`).
-   Click "Finish". Eclipse imports the project, and displays it in the
    Package Explorer pane.
-   Right-click the project, and select "Properties".
-   Under "C/C++Build->Environment", add an environment variable
    `LIQUIDFUN_SRC_PATH` that points to the LiquidFun folder (e.g.
    `liquidfunpaint/liquidfun/Box2D`). Alternatively, you can set
    `LIQUIDFUN_SRC_PATH` in your system environment then launch Eclipse after.
-   Apply the settings and quit the dialog box.
-   Right-click the project, and select "Run->Run As->Android Application"
    from the menu.
-   If you do not have a physical device, you must define a virtual one.
    For details on how to define a virtual device, see [managing avds][]. We
    do not recommend a virtual device for development, as the application
    relies heavily on the accelerometer and multi-touch inputs.
-   If the target is a physical device, unlock the device and observe the
    application executing.


  [Android SDK]: http://developer.android.com/sdk/index.html
  [Android NDK]: http://developer.android.com/tools/sdk/ndk/index.html
  [NDK Eclipse plugin]: http://developer.android.com/sdk/index.html
  [managing avds]: http://developer.android.com/tools/devices/managing-avds.html
  [ADT]: http://developer.android.com/tools/sdk/eclipse-adt.html
  [Apache Ant]: http://ant.apache.org/
  [SWIG]: http//www.swig.org

