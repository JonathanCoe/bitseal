Building Bitseal
=====================

Overview
------
Bitseal is currently developed and built using Eclipse: [https://eclipse.org](https://eclipse.org)

In the future, development and building will probably be migrated over to Android Studio: [https://developer.android.com/sdk/installing/studio.html](https://developer.android.com/sdk/installing/studio.html)

At the moment, build instructions are only available for use with Eclipse. In the future, builds from the command line should be possible using Grade, which is integrated into Android Studio. 


Setting up a Development Environment
------

1) Make sure you have a Java JDK installed. The Android team recommends the Oracle JDK is over the Open JDK.

2) Download the Android SDK: [https://developer.android.com/sdk/index.html#download](https://developer.android.com/sdk/index.html#download)

3) Download and install Eclipse: [https://www.eclipse.org/downloads](https://www.eclipse.org/downloads)

4) Install the Android ADT plugin: [http://developer.android.com/sdk/installing/installing-adt.html](http://developer.android.com/sdk/installing/installing-adt.html)

5) Run the Android SDK manager through Eclipse and download any updates available

6) If it is not already installed, install [git](http://git-scm.com)

7) Clone the Bitseal Github repository to your system: git clone https://github.com/JonathanCoe/bitseal.git

8) Import bitseal into Eclipse using the menu option File...Import...Import existing Android code

9) If you are on 64 bit linux you may encounter build errors at this stage. You can try to fix these by installing some extra packages: sudo apt-get install lib32stdc++6 lib32z1 lib32ncurses5 lib32bz2-1.0

10) Bitseal should now be ready to be built! In Eclipse, go to "Project....Build project"

12) The file bitseal.apk should now be created in the /bin folder of the Eclipse project. 
