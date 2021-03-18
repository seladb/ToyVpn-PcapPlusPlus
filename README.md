# ToyVpn-PcapPlusPlus

This project demonstrates how PcapPlusPlus can be used in Android apps.
It is built on top of [Android's ToyVpn Example](https://android.googlesource.com/platform/development/+/master/samples/ToyVpn) and adds a layer of network traffic analysis for the packets processed by ToyVPN.


## Build and run instructions

Please follow these guidelines to get a working version of this app:

### Step 0: Clone ToyVpn-PcapPlusPlus with submodules
When you clone this project from GitHub please make sure to clone it with submodules:
```shell
git clone --recurse-submodules https://github.com/seladb/ToyVpn-PcapPlusPlus
```

### Step 1: Build PcapPlusPlus for Android
- Prerequisites:
  - These steps should run on a Linux machine
  - Make sure you have [Android NDK](https://developer.android.com/ndk) installed
- Clone the PcapPlusPlus repo to some location on your machine: `git clone https://github.com/seladb/PcapPlusPlus /my/path/`
- Use the `build_pcapplusplus.sh` script to build PcapPlusPlus. This script has the following mandatory arguments:
    ```shell
    seladb@ubunu2004:~/ToyVpn-PcapPlusPlus$ ./build_pcapplusplus.sh -h
    
    Help documentation for build_pcapplusplus.sh.
    
    
    Basic usage: build_pcapplusplus.sh [-h] [--ndk-path] [--pcapplusplus-path] [--target]
    
    The following switches are recognized:
    --ndk-path             --The path of Android NDK, for example: '/opt/Android/Sdk/ndk/22.0.7026061'
    --pcapplusplus-path    --The path of PcapPlusPlus source code
    --target               --Build for specific target, should be one of: arm64-v8a, armeabi-v7a, x86, x86_64
    --help|-h              --Displays this help message and exits. No further actions are performed
    ```

- Set `--ndk-path` to your Android NDK path
- Set `--pcapplusplus-path` to the path where you clone PcapPlusPlus
- The `--target` is not mandatory. Use it only if you want to build PcapPlusPlus for a specific target
- The script takes a few minutes to run and will build PcapPlusPlus libraries for API version 29 on all 4 targets (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`)
- The libraries and PcapPlusPlus header files will be copied to this location: `app/libs/pcapplusplus`

__TBD__: in the next release of PcapPlusPlus Android will be officially supported so you'll be able to take the pre-compiled libraries directly from PcapPlusPlus release page instead of building them yourself

### Step 2: Build and run the server
ToyVpn has 2 parts:
- The Android app
- The server which serves as a simple VPN gateway

The app connects to the server which in turn connects to the outside world.
In order to run the app you need to first build and run the server:
- The server needs to run on a Linux machine
- Go to `server/linux`
- Build the server by running `make`
- As mentioned in [ToyVpnServer.cpp](https://github.com/seladb/ToyVpn-PcapPlusPlus/blob/master/server/linux/ToyVpnServer.cpp) Before you run the server you need to set up a TUN device:
    ```
    # Enable IP forwarding
    echo 1 > /proc/sys/net/ipv4/ip_forward

    # Pick a range of private addresses and perform NAT over eth0.
    iptables -t nat -A POSTROUTING -s 10.0.0.0/8 -o eth0 -j MASQUERADE

    # Create a TUN interface.
    ip tuntap add dev tun0 mode tun

    # Set the addresses and bring up the interface.
    ifconfig tun0 10.0.0.1 dstaddr 10.0.0.2 up  
    ```
- Now you can run the server:
    ```shell
    ./ToyVpnServer tun0 8000 test -m 1400 -a 10.0.0.2 32 -d 8.8.8.8 -r 0.0.0.0 0
    ```

### Step 3: Build and run the app
Now you're ready to build the app:

```shell
chmod +x gradlew
./gradlew assembleDebug
```


