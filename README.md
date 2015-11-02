Growl
=====

Cross platform notification popups, similar to "Growl" on OSX, "Toasts" on Windows, and Notifications on Ubuntu.

This small library is displays notifications on any screen, in any corner.

Primary Features:

1. Can specify which screen 
2. Can specify which corner, center is also possible
3. If no location is specified, it will show on whatever screen the mouse is on.
4. Duration timeouts
5. Light or Dark styles
6. Can close via close button or clicking on notification body
7. Can show/hide the close button
8. Can register a callback for when a user clicks on the notification body
9. Animates to a collated position if multiple notifications are in the same position


- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+

```
Growl.IMAGE_PATH    (type String, default value 'resources')
 - Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
```

![growl-light image](https://raw.githubusercontent.com/dorkbox/Growl/master/growl-light.png)

![growl-dark image](https://raw.githubusercontent.com/dorkbox/Growl/master/growl-dark.png)

