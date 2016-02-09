Growl
=====

Cross platform notification popups, similar to "Growl" on OSX, "Toasts" on Windows, and Notifications on Ubuntu.

This small library is displays notifications on any screen, in any corner, using swing.

Primary Features:

1. Can specify which screen 
2. Can specify which corner, center is also possible
3. If no location is specified, it will show on whatever screen the mouse is on.
4. Duration timeouts, with progress indicator on notification
5. Light or Dark styles
6. Can close via close button or clicking on notification body
7. Can show/hide the close button
8. Can register a callback for when a user clicks on the notification body
9. Animates to a collated position if multiple notifications are in the same position
10. Bypasses the EDT, and now renders at a beautiful 30 frames-per-second.


- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+
- You will need the images in the 'resources' directory, in addition to the normal libs.

```
Customization parameters:

-ActiveRenderLoop.TARGET_FPS  (type int, default value '30')
 - Allows you to customize the delay (for hiding the popup) when the cursor is "moused out" of the popup menu

-OS.FORCE_HIGH_RES_TIMER  (type boolean, default value 'true')
 - By default, the timer resolution in some operating systems are not particularly high-resolution (ie: 'Thread.sleep(1)' will not really
  sleep for 1ms, but will really sleep for 16ms). This forces the JVM to use high resolution timers.

Growl.IMAGE_PATH    (type String, default value 'resources')
 - Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
```

![growl-light image](https://raw.githubusercontent.com/dorkbox/Growl/master/growl-light.png)

![growl-dark image](https://raw.githubusercontent.com/dorkbox/Growl/master/growl-dark.png)


<h2>License</h2>

This project is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.
