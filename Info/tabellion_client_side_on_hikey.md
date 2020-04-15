# Table of Contents

- [Tabellion Components Overview](Info/tabellion_components_overview.md#tabellion-components-overview)

- [Tabellion client side on HiKey](Info/tabellion_client_side_on_hikey.md#tabellion-client-side-on-hikey)

    - [Building Xen on ARM](Info/tabellion_client_side_on_hikey.md#building-xen-on-arm)

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

