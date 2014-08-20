android-white-label-app
=======================

Android White Label Application for the Voting Information Project

Requires: Android Studio v0.8.0+


##Build Status
![Build Status](https://travis-ci.org/votinginfoproject/android-white-label-app.svg?branch=develop)

Dependencies:
-------------

Min SDK Version: 15 (Ice Cream Sandwich 4.0.3)
Target SDK Version: 19 (Android KitKat)
Android Support Library v4 revision 19.1


Building/Running the app:
-------------------------

#### Setting up the Java Environment

Install the [Java SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) on your system.


#### Set up Git Repository

*  Install git.  For Windows users, you may wish to do so by installing [GitHub for Windows](https://windows.github.com/).  For Mac users, there is [GitHub for Mac](https://mac.github.com/).

*  Clone the project repository.  To do so with the GitHub application, click the plus sign in the upper left, and select to clone a repository.

    -  The project repository URL to clone with public access is:
    [https://github.com/votinginfoproject/android-white-label-app.git](https://github.com/votinginfoproject/android-white-label-app.git)


#### Installing Android SDK Dependencies

*  Install [Android Studio](https://developer.android.com/sdk/installing/studio.html).

*  Launch Android Studio and select to 'Import Project' from the project directory cloned to above.

*  Then, in Android Studio, go to Tools->Android->SDK Manager and
install the following:

  -  Tools -> Android SDK Build-tools 19.1
  -  Android 4.4.2K (API V19) -> SDK Platform and Google APIs
       - For running in an emulator, install one or both system images.  The Intel x86 system image will run much faster than ARM, especially if hardware accelleration is enabled.
  -  Extras -> Android Support Library
  -  Extras -> Android Support Repository
  -  Extras -> Google Play Services (v.19)
  -  Extras -> Google Repository
  -  For a faster emulator on Windows, Extras -> Intel x86 Emulator Accelerator


#### Install device drivers and enable debug mode on device

*  Windows users will need to install the appropriate USB driver for their device in order to run the app on a device.  Please see the [list of available USB drivers](http://developer.android.com/tools/extras/oem-usb.html) and installation instructions.

* You can [enable debug mode on your device](http://developer.android.com/tools/device.html) to allow debugging over USB.


#### Running in an emulator

The app will run on an Android emulator.  However, the map, directions, and distances to polling locations will not be available within the emulator, as these features depend on Google Play services, which are not available from within the emulator.

The emulator will run much faster with hardware acceleration enabled.  Please see the [directions on using the Android emulator and enabling hardware acceleration](http://developer.android.com/tools/devices/emulator.html).  For Windows, this uses the HAXM emulator accellerator package available under 'Extras' in the SDK Manager.

#### Adding API keys for the app

*  Copy the `api_keys.xml.template` file from the `xmltemplates` directory to `app/src/main/res/values/api_keys.xml`.

    - On Windows, in the project directory at the command prompt, enter:
    
        copy xmltemplates\api_keys.xml.template /A app\src\main\res\valus\api_keys.xml /A

    - For Mac or Linux, in the project directory in the console, enter:

        cp xmltemplates/api_keys.xml.template app/src/main/res/values/api_keys.xml

*  Edit api_keys.xml to add in your API keys from the [Google Developer Console](https://console.developers.google.com).

    -  Note that the Google Maps API key is the Anroid key, and the Civic Info API key is the browser key.


#### Running the app

Sync your gradle build file if your IDE asks you to, and then run the app via Run -> Run.


Testing the app
---------------

#### Creating the VIPAndroidTests Run Configuration

*  In Android Studio, go to Run -> Edit Configurations.

*  In the window that appears, click the green '+' in the top left and select 'Android Tests'
from the dropdown. 

*  In the box on the right, rename the configuration 'Unnamed' to 'VIPAndroidTests'.
Ensure under the General tab that Module is set to 'app' and the
selected radio button for the Tests option is 'All in Module'.
In the Target Device section, ensure that 'Show chooser dialog' is selected, this will allow
you to select the device to run the tests on at runtime.  Optionally check 'Use same device'
if you generally only use one device/emulator to test on.


#### Running Tests

*  Ensure that the VIPAndroidTests run configuration is selected in the dropdown in the
second toolbar at the top of Android Studio, then select Run->Run or click the 'Run' button 
just to the right of the run configuration dropdown.

*  Android Studio will display a green "Tests Succeeded" or a red "Tests Failed" popup 
in the bottom center of the IDE on test completion.

