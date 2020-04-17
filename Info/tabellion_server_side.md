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

# Tabellion Server Side

## Setting up Environment

Our Tabellion Server requires many libraries and tools. Some of them may not be listed here, which will need you to install before you can successfully run Tabellion Server.

Assuming the environment is a Debian liked environment. (Our server is tested on Ubuntu 18.04)

First you will need to install Apache 2, which you can find instruction in this webpage: https://www.digitalocean.com/community/tutorials/how-to-install-the-apache-web-server-on-ubuntu-18-04

Then please follow the instruction in this webpage to set your Apache 2 default folder to Tabellion Server folder: https://www.digitalocean.com/community/tutorials/how-to-move-an-apache-web-root-to-a-new-location-on-ubuntu-16-04

We are using MySQL as our Tabellion Server's databse. Please follow the instruction in this webpage to install MySQL and set up your MySQL account: https://www.digitalocean.com/community/tutorials/how-to-install-mysql-on-ubuntu-18-04

Intel Software Guard Extension(SGX) is needed for running our enclave, please make sure your server is compatiable with at least SGX 1. Then follow this webpage to install the SGX driver: https://github.com/intel/linux-sgx (Remember to source the SDK once you finish installing it)

In addition, for ruuning our enclave, you will need sudo permission. Please give sudo permission to the following PHP file: check_sign_and_process_contract.php. This webpage contains instruction on how to give PHP file sudo permission: http://www.bonebrews.com/granting-sudo-to-php/

For simplicity, you can also just follow the following steps to give all www-data sudo permission. (This could be risky for your server)

Open the sudoers file:

```
sudo visudo
```

Add the folloing line and save the file:

```
www-data ALL=NOPASSWD: ALL
```

## Configuring Tabellion Server

First we need to set up permissions on Tabellion Server.

Run the following line to give access permissions to all files and folders in Tabellion Server:

```
sudo chmod -R 755 [path to Tabellion Server]
```

Run all the following lines to give write permissions to needed folders.

```
sudo chmod -R 777 [path to Tabellion Server]/submitted_files
sudo chmod -R 777 [path to Tabellion Server]/users
```

Then we need to set up database access in all Python files(*.py).

In most of Python files under Tabellion Server root folder (except the ones started with FCM***), you will find the following lines of codes:

```
mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)
```

Please replace these lines with your own MySQL's host, username, password and database name.

Finally we need to set up Google FCM Service on Tabellion Server. Please follow the official FCM instruction to set up certificate on Tabellion Server. Then open all Python files started with FCM***, which you will find the folling line:

```
cred = credentials.Certificate('truesignresearch-firebase-adminsdk-gl0ko-0e85616485.json')
```

Replace it with your own certificate's name.
