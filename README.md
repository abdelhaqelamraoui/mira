# Mira - Minimalist Camera & Magnifier

**Mira** is a simple, full-screen camera application for Android, designed to be a "digital magnifying glass." It launches directly into the camera view, keeps the screen on, and provides essential features with a clean, distraction-free interface.

## Features

-   **Instant Camera Access**: The app opens directly to the live camera preview, with no extra menus or buttons to navigate.
-   **Keep Screen On**: The screen will not dim or go to sleep while the app is open, making it perfect for continuous viewing.
-   **Pinch-to-Zoom**: Intuitively zoom in and out using a standard two-finger pinch gesture, turning your phone into a powerful magnifying glass.
-   **Tap-to-Focus**: Simply tap anywhere on the screen to instantly refocus the camera on that specific point.
-   **One-Time Flashlight Prompt**: On first launch, the app asks if you'd like to turn on the flashlight. The choice is made once and the dialog disappears, keeping the UI clean for the rest of the session.

## Core Purpose

The primary goal of Mira is to provide an immediate and unobstructed camera view. It's built for situations where you need a quick magnifying tool without the complexity of a standard camera app. Use it to:
-   Read tiny text on labels and documents.
-   Get a closer look at small objects.
-   Examine details in hard-to-reach places.

## Tech Stack & Implementation

This application is built with modern Android development tools, showcasing a simple yet powerful architecture.

-   **Language**: [Kotlin](https://kotlinlang.org/)
-   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a fully declarative and modern UI.
-   **Camera API**: [CameraX](https://developer.android.com/training/camerax), the recommended Jetpack library for camera development. It simplifies camera operations like preview, focus, and zoom while managing the camera lifecycle automatically.
-   **Architecture**: A single-activity architecture where Jetpack Compose controls the entire UI state, including permission handling and dialogs.

## How to Build

1.  Clone this repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync the project dependencies. The main dependencies include:
    -   `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`
    -   `androidx.compose` for UI
    -   `androidx.activity:activity-compose`
4.  Build and run the project on a physical Android device.

## Permissions

The app requires the `CAMERA` permission to function. This is declared in the `AndroidManifest.xml` and requested at runtime when the app first starts.
