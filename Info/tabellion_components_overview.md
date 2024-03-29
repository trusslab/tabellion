# Table of Contents

- [Tabellion Overview](../README.md#tabellion-overview)

- [People](../README.md#people)

- [Tabellion Components Overview](tabellion_components_overview.md#tabellion-components-overview)

- [Tabellion client side on HiKey](tabellion_client_side_on_hikey.md#tabellion-client-side-on-hikey)

    - [Building Xen on ARM](tabellion_client_side_on_hikey.md#building-xen-on-arm)

    - [Building AOSP with Xen enabled](tabellion_client_side_on_hikey.md#building-aosp-with-xen-enabled)

    - [Preparing your boot image files](tabellion_client_side_on_hikey.md#preparing-your-boot-image-files)

    - [Flashing your image](tabellion_client_side_on_hikey.md#flashing-your-image)

    - [Building OPTEE](tabellion_client_side_on_hikey.md#building-optee)

    - [Integrating with Tabellion](tabellion_client_side_on_hikey.md#integrating-with-tabellion)
    
- [Tabellion Android Application](tabellion_android_application.md#tabellion-android-application)

    - [Configuring Tabellion App](tabellion_android_application.md#configuring-tabellion-app)

    - [Building Tabellion App using Android Studio](tabellion_android_application.md#building-tabellion-app-using-android-studio)

    - [Installing Tabellion App on Android Phone](tabellion_android_application.md#installing-tabellion-app-on-android-phone)

    - [Tabellion App and Root Permission](tabellion_android_application.md#tabellion-app-and-root-permission)

    - [Using Tabellion App on Android Phone](tabellion_android_application.md#using-tabellion-app-on-android-phone)

- [Tabellion Server Side](tabellion_server_side.md#tabellion-server-side)

    - [Setting up Environment](tabellion_server_side.md#setting-up-environment)

    - [Configuring Tabellion Server](tabellion_server_side.md#configuring-tabellion-server)

# Tabellion Components Overview

In Tabellion, we have developed the client side on HiKey development board and Nexus 5X smartphone and the server side in an Azure VM.
For the client side, we have implemented the secure primitives (secure photo, secure screenshot, secure timestamp) using the HiKey development board.
We have developed an Android Application for the user study.

We will first cover the implementation on the HiKey board.
Next, we will introduce the Android app and the server side.