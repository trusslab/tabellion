This is the source code for our project Tabellion that we published in ACM MobiSys'20. You can find the PDF of the paper in here: Link.
Title of the paper: Tabellion: Secure Legal Contracts on Mobile Devices.

# People
------
* [Saeed Mirzamohammadi, CS Grad, UC Irvine (Lead Student)] (http://www.ics.uci.edu/~saeed)
* [Yuxin (Myles) Liu, CS Undergrad, UC Irvine] (https://www.linkedin.com/in/mylesliu)
* [Ardalan Amiri Sani, CS Faculty, UC Irvine] (http://www.ics.uci.edu/~ardalan)
* [Sharad Agarwal, Principal Researcher, Microsoft Research] (https://sharadagarwal.net)
* [Sung Eun (Summer) Kim, Law Faculty, UC Irvine] (https://www.law.uci.edu/faculty/full-time/kim)

# 1. Tabellion Components Overview

In Tabellion, we have developed the client side on HiKey development board and Nexus 5X smartphone and the server side in an Azure VM.
For the client side, we have implemented the secure primitives (secure photo, secure screenshot, secure timestamp) using the HiKey development board.
We have developed an Android Application for the user study.

We will first cover the implementation on the HiKey board.
Next, we will introduce the Android app and the server side.

## 1.1 Tabellion client side on HiKey

### 1.1.1 Building Xen on ARM:

First, you need to clone the Xen repo and cross compile it for ARM.
You can use this command the clone Xen:

```sh
git clone git://xenbits.xen.org/xen.git
```

Cross compile Xen using the instructions in here:
https://wiki.xenproject.org/wiki/Xen_ARM_with_Virtualization_Extensions#Building_Xen_on_ARM
For this project, git checkout the tag version: staging-4.7 or a higher version.

You can follow these steps for building:

```sh
cd xen
# make clean # You might need this from time to time!
export CROSS_COMPILE=aarch64-linux-gnu-
export ARCH=arm64
export XEN_TARGET_ARCH=arm64
make defconfig
make -j8
```

### 1.1.2 Building AOSP with Xen enabled:

Follow the exact instructions here to build AOSP for HiKey:
https://github.com/linaro-swg/optee_android_manifest/tree/hikey-n-4.9-master#37-run-the-rest-of-the-aosp-build-for-an-8gb-board-use

Before building your AOSP, you need to enable support for Xen in the AOSP kernel:
https://wiki.xenproject.org/wiki/Mainline_Linux_Kernel_Configs#Configuring_the_Kernel

For the HiKey board, you need to find "hikey_defconfig" file for arm64 and add the Dom0 support to the kernel as above link.

### 1.1.3 Preparing your boot image files:

Now you need to modify grub config file to boot the Xen image you already built before.

Grab boot_fat.uefi.img from your AOSP directory located in "your_aosp_dir/device/linaro/hikey/installer/hikey" and copy it wherever you would like (just to keep the original intact as backup since we need to modify this image). 

Make a temp folder and mount your boot image there:

```sh
sudo mount -o loop,rw,sync boot_fat.uefi.img your_temp_dir
```

You will need to make the following changes to the image:

- Copy your compiled Xen binary to the image dir:

```sh
sudo cp your_xen_dir/xen/xen your_temp_dir
```

- Add the .efi extension to Xen (can just rename):
```sh
sudo mv your_temp_dir/xen your_temp_dir/xen.efi
```

- Add your Xen configuration file (must be named xen.cfg):

```sh
sudo cp xen.cfg your_temp_dir
```

You can look at my xen.cfg as a reference:

```sh
options=console=dtuart dom0_mem=2048M loglvl=all guest_loglvl=all dtuart=/soc/uart@f7113000
kernel=kernel console=ttyAMA3,115200 androidboot.console=ttyAMA3 fiq_debugger.disable=Y
androidboot.hardware=hikey androidboot.selinux=0 firmware_class.path=/system/etc/firmware
efi=noruntime printk.devkmsg=on androidboot.serialno=0123456789

dtb=hi6220-hikey.dtb
ramdisk=ramdisk.img
```

- Edit grub.cfg in EFI/BOOT/ within the boot image to have it load Xen:

```sh
sudo vi your_temp_dir/EFI/BOOT/grub.cfg
```

and add the following as an entry:

```sh
menuentry 'AOSP-Xen' {
      search.fs_label boot root
      chainloader ($root)/xen.efi
}
```

- Then change "set default = 0" on the top to the correct index of where you placed the new grub entry to default load this.

### 1.1.4. Flashing your image:

- Unmount the image:

```sh
sudo umount your_temp_dir
```

- Flash the boot image to HiKey:

```sh
sudo fastboot flash boot boot_fat.uefi.img
```

* Note: Follow the steps for building AOSP to the end, then flash the new boot image with Xen support the next time.
For flashing the boot image on HiKey, set the jumper configurations as mentioned in the link.


### 1.1.5. Building OPTEE:

For building OPTEE, we use the OPTEE built for Oreo AOSP as the one in the previous link is outdated and might not boot:
https://github.com/linaro-swg/optee_android_manifest/tree/lcr-ref-hikey-o

Build the bootloader firmware (fip.bin):

```sh
pushd device/linaro/hikey/bootloader
make TARGET_TEE_IS_OPTEE=true #make sure build is successful
popd
cp out/dist/fip.bin device/linaro/hikey/installer/hikey/
cp out/dist/l-loader.bin device/linaro/hikey/installer/hikey/
```

To flash your fip.bin:

```sh
sudo fastboot flash fastboot fip.bin
```

### 1.1.6. Integrating with Tabellion:

Now that you have set up your directories, git checkout the Tabellion repos, re-build the kernels, and re-flash the images.

For AOSP kernel:

```sh
git add remote origin https://github.com/trusslab/tabellion_hikey_android
git checkout Tabellion_v1
```

For Xen:

```sh
git add remote origin https://github.com/trusslab/tabellion_hikey_xen
git checkout Tabellion_v1
```

For OPTEE:

```sh
git add remote origin https://github.com/trusslab/tabellion_hikey_optee
git checkout Tabellion_v1
```



## 1.2 Tabellion Android Application

XXX


## 1.3 Tabellion Server side

XXX


# 2. ChangeLog

* April, 2020: version 1.0 is released.

