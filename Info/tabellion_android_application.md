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

# Tabellion Android Application

## Configuring Tabellion App

Open ".../TabellionApp/app/src/main/java/edu/uci/ics/charm/tabellion/Connection.java".

On line 54, you will see:

```
private static final String mainUriTag = "http://13.90.224.167/OpenSourceTestVersion/tabellion/TabellionServer/";
```

Please change it according to the following format:

```
private static final String mainUriTag = "http://[Your server address along with Tabellion Server's root folder]";
```

We are using Google FCM as our Notification Service. In order to set it up, you will need to first register an Google FCM account:

https://firebase.google.com/docs/cloud-messaging

Then you can follow the official instruction and replace the certificate in Tabellion App.

## Building Tabellion App using Android Studio

Please build the app with latest Android Studio (https://developer.android.com/studio).

For how to build an Android App, please check these pages out:

https://developer.android.com/training/basics/firstapp

https://developer.android.com/studio/run

## Installing Tabellion App on Android Phone

For our Android App prototype, we tested it on Android 8, 9 and 10 with Nexus 5X.

Our App need to be installed on Android version 8.0 or above.

After building the App, you can download it to your Android phone and install it or sideload it your phone directly.

## Tabellion App and Root Permission

You don't need Root Permission to use the App, but without Root Permission, you will be unable to use any LED related functions.

## Using Tabellion App on Android Phone

Please first give all needed permissions to the App before using it.

You will need to register at least two accounts in order to perform a basic test. (note that the two accounts need to be registered and used in two different phones)

Use account A to create an contract with account B and sign it (offeror); then use account B to sign the contract. (note that there are additional confirmations needed before signing)

