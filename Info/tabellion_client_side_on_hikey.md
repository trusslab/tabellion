# Table of Contents

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

- [Tabellion Server Side](Info/tabllion_server_side.md#tabellion_server_side)

    - [Setting up Environment](Info/tabllion_server_side.md#setting-up-environment)

    - [Configuring Tabellion Server](Info/tabllion_server_side.md#configuring-tabellion-server)

# Tabellion client side on Hikey

## Building Xen on ARM

First, you need to clone the Xen repo and cross compile it for ARM.
You can use this command the clone Xen:

```
git clone git://xenbits.xen.org/xen.git
```

Cross compile Xen using the instructions in here:
https://wiki.xenproject.org/wiki/Xen_ARM_with_Virtualization_Extensions#Building_Xen_on_ARM
For this project, git checkout the tag version: staging-4.7 or a higher version.

You can follow these steps for building:

```
cd xen
# make clean # You might need this from time to time!
export CROSS_COMPILE=aarch64-linux-gnu-
export ARCH=arm64
export XEN_TARGET_ARCH=arm64
make defconfig
make -j8
```

## Building AOSP with Xen enabled

Follow the exact instructions here to build AOSP for HiKey:
https://github.com/linaro-swg/optee_android_manifest/tree/hikey-n-4.9-master#37-run-the-rest-of-the-aosp-build-for-an-8gb-board-use

Before building your AOSP, you need to enable support for Xen in the AOSP kernel:
https://wiki.xenproject.org/wiki/Mainline_Linux_Kernel_Configs#Configuring_the_Kernel

For the HiKey board, you need to find "hikey_defconfig" file for arm64 and add the Dom0 support to the kernel as above link.

## Preparing your boot image files

Now you need to modify grub config file to boot the Xen image you already built before.

Grab boot_fat.uefi.img from your AOSP directory located in "your_aosp_dir/device/linaro/hikey/installer/hikey" and copy it wherever you would like (just to keep the original intact as backup since we need to modify this image). 

Make a temp folder and mount your boot image there:

```
sudo mount -o loop,rw,sync boot_fat.uefi.img your_temp_dir
```

You will need to make the following changes to the image:

- Copy your compiled Xen binary to the image dir:

```
sudo cp your_xen_dir/xen/xen your_temp_dir
```

- Add the .efi extension to Xen (can just rename):

```
sudo mv your_temp_dir/xen your_temp_dir/xen.efi
```

- Add your Xen configuration file (must be named xen.cfg):

```
sudo cp xen.cfg your_temp_dir
```

You can look at my xen.cfg as a reference:

```
options=console=dtuart dom0_mem=2048M loglvl=all guest_loglvl=all dtuart=/soc/uart@f7113000
kernel=kernel console=ttyAMA3,115200 androidboot.console=ttyAMA3 fiq_debugger.disable=Y
androidboot.hardware=hikey androidboot.selinux=0 firmware_class.path=/system/etc/firmware
efi=noruntime printk.devkmsg=on androidboot.serialno=0123456789

dtb=hi6220-hikey.dtb
ramdisk=ramdisk.img
```

- Edit grub.cfg in EFI/BOOT/ within the boot image to have it load Xen:

```
sudo vi your_temp_dir/EFI/BOOT/grub.cfg
```

and add the following as an entry:

```
menuentry 'AOSP-Xen' {
      search.fs_label boot root
      chainloader ($root)/xen.efi
}
```

- Then change "set default = 0" on the top to the correct index of where you placed the new grub entry to default load this.

## Flashing your image

- Unmount the image:

```
sudo umount your_temp_dir
```

- Flash the boot image to HiKey:

```
sudo fastboot flash boot boot_fat.uefi.img
```

* Note: Follow the steps for building AOSP to the end, then flash the new boot image with Xen support the next time.
For flashing the boot image on HiKey, set the jumper configurations as mentioned in the link.

## Building OPTEE

For building OPTEE, we use the OPTEE built for Oreo AOSP as the one in the previous link is outdated and might not boot:
https://github.com/linaro-swg/optee_android_manifest/tree/lcr-ref-hikey-o

Build the bootloader firmware (fip.bin):

```
pushd device/linaro/hikey/bootloader
make TARGET_TEE_IS_OPTEE=true #make sure build is successful
popd
cp out/dist/fip.bin device/linaro/hikey/installer/hikey/
cp out/dist/l-loader.bin device/linaro/hikey/installer/hikey/
```

To flash your fip.bin:

```
sudo fastboot flash fastboot fip.bin
```

## Integrating with Tabellion

Now that you have set up your directories, git checkout the Tabellion repos, re-build the kernels, and re-flash the images.

For AOSP kernel:

```
git add remote origin https://github.com/trusslab/tabellion_hikey_android
git checkout Tabellion_v1
```

For Xen:

```
git add remote origin https://github.com/trusslab/tabellion_hikey_xen
git checkout Tabellion_v1
```

For OPTEE:

```
git add remote origin https://github.com/trusslab/tabellion_hikey_optee
git checkout Tabellion_v1
```

