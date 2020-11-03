# Network Survey Plus Android App

[![Build Status](https://travis-ci.com/christianrowlands/android-network-survey.svg?branch=develop)](https://travis-ci.com/github/christianrowlands/android-network-survey-plus)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg?style=flat)](https://github.com/christianrowlands/android-network-survey-rooted/blob/develop/LICENSE)

## What is it?

Network Survey+ is the Rooted version of the [Network Survey Android App](https://github.com/christianrowlands/android-network-survey).


## Why?

Why do we need a rooted version of the Network Survey Android app? Well, the unrooted version can only get
access to a limited set of cellular details. With root access on certain devices with the right Qualcomm
chipset, this app can get access to detailed cellular network messages via Qualcomm Diagnostic Monitor (QCDM).

This app logs several messages coming from QCDM to a pcap file for follow on processing.


## Supported Devices

In theory, any Android device with a Qualcomm chip should work, but it seems that only certain devices work as expected.
The following is a list of devices that have been confirmed to work with this Network Survey+ app.

| Device        | Comments      |
| ------------- | ------------- |
| Pixel 3a      |               |


## Build and development instructions
#### Basic Instructions

To build and install the project follow the steps below:

    1) Clone the repo.
    2) Open Android Studio, and then open the root directory of the cloned repo.
    3) Connect an Android Phone (make sure debugging is enabled on the device).
    4) Install and run the app by clicking the "Play" button in Android Studio.

#### Detailed Instructions

This app included a native application written in C (called Diag Revealer) that is used to initialize the /dev/diag 
QCDM device on the phone, and stream the output from the /dev/diag device to the a FIFO named pipe. The Java portion of 
this app reads from that FIFO queue and consumes the QCDM messages.

The Diag Revealer application source code was pulled from the MobileInsight Android app source code.

To make things easy, the full compiled binary of the Diag Revealer app is included in this repo. Eventually, we will 
make the gradle build script compile the Diag Revealer binary, but for now it is included in the repo. If you want to 
make changes to the Diag Revealer C application, you can do so and then compile it using the following steps:

 1) `cd app`
 1) `ndk-build`
 1)  Add the `.so` extension to the output file (probably something like `libs/arm64-v8a/diag_revealer` to `libs/arm64-v8a/libdiag_revealer.so`)
 
The reason the file needs to have the `.so` extension is that Android only unpacks the native application if it is a 
shared library instead of just a regular executable. At some point it would be nice to investigate if we can get around
this and leave off the .so extension since it is misleading.


### Prerequisites

Install Android Studio to work on this code.


## Related Projects

This project is not alone in trying to leverage QCDM to get access to low level cellular messages. Following
are a list of other projects that might be of interest to you.
 * [QCSuper](https://github.com/P1sec/QCSuper)
 * [SnoopSnitch](https://opensource.srlabs.de/projects/snoopsnitch)
 * [Mobile Sentinel](https://github.com/RUB-SysSec/mobile_sentinel)
 * [MobileInsight](https://github.com/mobile-insight/mobileinsight-mobile)


## Changelog

##### [0.1.2](https://github.com/christianrowlands/android-network-survey-rooted/releases/tag/v0.1.2) - 2020-11-02
 * Fixed a bug where the diag_revealer.so file was not being unpacked.

##### [0.1.0](https://github.com/christianrowlands/android-network-survey-rooted/releases/tag/v0.1.0) - 2020-10-28
 * Initial Release of Network Survey Plus.


## Contact

* **Christian Rowlands** - [Craxiom](https://github.com/christianrowlands)
