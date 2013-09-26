# Introduction

[About](#about)<br/>
[Prerequisites](#pre)<br/>
[About this manual](#atm)<br/>
[Feedback and reporting bugs](#frb)<br/>
[Core concepts](#cc)<br/>
[Modules](#mo)<br/>


<a name="About"></a><br/>

## About

LiquidFun Paint is a simple painting game that showcases the fluid simulation in
[LiquidFun], with a polished look. The primary interaction of the player with
the game is through the touch screen and the accelerometer built into the
Android device.

With the touch screen, the player can draw multiple types of fluid particles,
erase drawn particles, and move the particles around.

With the accelerometer, the player can tilt the device and change the perceived
gravity, which will then cause the fluids to react and move towards the gravity
pull.

The game is written in Java, using the [SWIG] component of [LiquidFun]. The
[SWIG] component will generate JNI (Java Native Interface) functions to
facilitate communication between C++ and Java. LiquidFun Paint also utilizes
Eclipse, and the [Android SDK] and [Android NDK], for UX design and for
building the various Java and C++ components.

<a name="pre"></a><br/>
## Prerequisites

LiquidFun Paint is based on [LiquidFun], a 2D fluid simulation library. Please
refer to the [LiquidFun] site for more information and a tutorial on the
library.

LiquidFun Paint is written in Java, and we recommend a working knowledge of the
language before attempting to use the source code. In addition, if you choose to
delve more into [LiquidFun], you will need to have additional knowledge in C++
and [SWIG]. LiquidFun Paint should not be your first Android programming
project! You should be comfortable with compiling, linking, and debugging.

<b>Caution</b>

LiquidFun Paint should not be your first Android or Java project. Please
learn Java programming, compiling, linking, and debugging, especially for
Android, before working with LiquidFun Paint. There are many resources for this
on the Internet.

<a name="atm"></a><br/>

## About this manual

This manual covers the basics of the LiquidFun Paint's code components. For
anything that this manual does not cover, please refer to the comments in the
source code.

This manual is only updated with new releases. The version in source control may
be out of date.

<a name="frb"></a><br/>
## Feedback and Reporting Bugs

If you have a question or feedback about LiquidFun Paint, please leave a comment
in the [Google group]. This is also a great place for community discussion.

LiquidFun Paint issues are tracked using GitHub.

Please file bugs and feature requests here:
https://github.com/google/liquidfunpaint/issues

Please provide as much detail as you can when posting.

<a name="cc"></a><br/>
## Core Concepts

LiquidFun Paint uses a model-view-controller model.

Both the model and the view components are owned by the `Renderer` class. We
need to synchronize between the physics simulation and the rendering. In order
to keep a simple structure, whenever `onDrawFrame()` is called from OpenGL, we
run the update/render loop.

The controller component is split into two classes -  `Controller` and
`MainActivity`. `Controller` implements listeners that respond to touch and
accelerometer events, and those will in turn be used to manipulate [LiquidFun].
`MainActivity` is an Android activity which listens to Android UI components,
and updates the `Controller` accordingly.

### Threading

As [LiquidFun] is not reentrant, we need to ensure thread safety from the
application itself. We initialize only one instance of `b2World` from
[LiquidFun], and within that, only one instance of `b2ParticleSystem`. We use
ReentrantLocks to ensure safe access to both instances from other threads.

### Memory Management

All [LiquidFun] objects are allocated through the C++ allocator. As a result,
they should be properly cleaned up when they are not in use anymore. Please
refer to the [LiquidFun SWIG documentation] for more details.

<a name="mo"></a><br/>
## Modules

### Renderer

We have multiple renderers for different rendering purposes.

-   Base Renderer

    This implements `GLSurfaceView.Renderer`, the basic OpenGL ES renderer in
    the Android SDK.

    -   `onSurfaceCreated`

    This is called when the OpenGL surface is created or recreated after
    the context is lost. We will recreate all OpenGL objects as well as pass
    down the call to other renderers.

    We also read JSON files here. The data from the JSON files are needed to
    initialize OpenGL objects.

    For further information on GLSurfaceView.Renderer, please visit
    http://developer.android.com/reference/android/opengl/GLSurfaceView.Renderer.html.

-   Debug Renderer

    This extends the `b2Draw` class from [LiquidFun], and allows for efficient
    debug rendering when prototyping. It is controllable via a static boolean
    constant.

-   Particle Renderer

    This renders the fluid particle state from [LiquidFun]. It goes through
    `ParticleGroups` in succession, and determines which specific particle
    renderer to use. We have two renderers, one for loose water-like particles,
    the other for rigid body or wall particles.

    The general flow for either type of particles is:

    -   Render particles as point sprites
    -   Horizontal and vertical blur pass (`BlurRenderer`)
    -   Copy water-like particles to screen with alpha thresholding
    (`ScreenRenderer`)
    -   Copy other particles to screen alpha thresholding (`ScreenRenderer`)

-   TextureRenderer

    This is a basic texture renderer for ease of initializing and rendering any
    textures to the OpenGL context.

### Controller

The controller component is split into two classes.

-   MainActivity

    This controls all the visible UI, include the buttons for players to change
    drawing mode (tools) and colors, as well as the reset button to reset the
    whole canvas.

-   Controller

    This intercepts touch events from the canvas, as well as accelerometer
    inputs, and passes the information onto the [LiquidFun] component. We have
    the following tools:

    - `PencilTool`: Draw particles that will not move or be affected by fluids.
    - `RigidTool`: Draw particles that will float, bob and move with the fluid.
    - `WaterTool`: Draw particles that flow, drip and slosh realistically.
    - `EraserTool`: Erases particles.
    - `MoveTool`: Move particles.



  [LiquidFun]: http://google.github.io/liquidfun/
  [SWIG]: http://www.swig.org
  [Android SDK]: http://developer.android.com/sdk/index.html
  [Android NDK]: http://developer.android.com/tools/sdk/ndk/index.html
  [LiquidFun SWIG documentation]: http://google.github.io/liquidfun/SWIG/html/index.html
  [Google group]: http://group.google.com/group/liquidfunpaint

