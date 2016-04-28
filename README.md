Notify
======

Cross platform notification popups, similar to "Growl" on OSX, "Toasts" on Windows, and "Notifications" on Ubuntu.

This small library is displays notifications on any screen, in any corner.

Primary Features:

1. Can specify which screen 
2. Can specify which corner, center is also possible
3. If no location is specified, it will show on whatever screen the mouse is on.
4. Duration timeouts, with progress indicator on notification
5. Light or Dark themes
6. Can close via close button or clicking on notification body
7. Can show/hide the close button
8. Can register a callback for when a user clicks on the notification body
9. Animates to a collated position if multiple notifications are in the same position
10. Bypasses the swing EDT, and now renders at a beautiful 30 frames-per-second.


- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+
- You will need the images in the 'resources' directory, in addition to the normal libs.

```
Customization parameters:

-ActiveRenderLoop.TARGET_FPS  (type int, default value '30')
 - Allows you to customize the delay (for hiding the popup) when the cursor is "moused out" of the popup menu

-OS.FORCE_HIGH_RES_TIMER  (type boolean, default value 'true')
 - By default, the timer resolution in some operating systems are not particularly high-resolution (ie: 'Thread.sleep(1)' will not really
  sleep for 1ms, but will really sleep for 16ms). This forces the JVM to use high resolution timers.

Notify.IMAGE_PATH    (type String, default value 'resources')
 - Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
```

![light theme](https://raw.githubusercontent.com/dorkbox/Notify/master/notify-light.png)

![dark theme](https://raw.githubusercontent.com/dorkbox/Notify/master/notify-dark.png)


<h4>We now release to maven!</h4> 

There is a hard dependency in the POM file for the utilities library, which is an extremely small subset of a much larger library; including only what is *necessary* for this particular project to function.

This project is **kept in sync** with the utilities library, so "jar hell" is not an issue. Please note that the util library (in it's entirety) is not added since there are **many** dependencies that are not *necessary* for this project. No reason to require a massive amount of dependencies for one or two classes/methods.  
```
<dependency>
  <groupId>com.dorkbox</groupId>
  <artifactId>Notify</artifactId>
  <version>2.12</version>
</dependency>
```

Or if you don't want to use Maven, you can access the files directly here:  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Notify/  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Notify-Dorkbox-Util/  

https://oss.sonatype.org/content/repositories/releases/com/dorkbox/TweenEngine/      
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/ObjectPool/  

https://repo1.maven.org/maven2/org/slf4j/slf4j-api/      


<h2>License</h2>

This project is © 2015 dorkbox llc, and is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.

