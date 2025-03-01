# Notify

Cross platform notification popups, similar to "Growl" on OSX, "Toasts" on Windows, and "Notifications" on Ubuntu.

This small library can display notifications on any screen, in any corner.

## Table of Contents

* [Installation](https://github.com/dorkbox/Notify#installation)
    * [Gradle](https://github.com/dorkbox/Notify#gradle)
    * [Maven](https://github.com/dorkbox/Notify#maven)
    * [Scala SBT](https://github.com/dorkbox/Notify#scala-sbt)
* [Basic Usage](https://github.com/dorkbox/Notify#basic-usage)
* [Features](https://github.com/dorkbox/Notify#features)
* [Release Notes](https://github.com/dorkbox/Notify#release-notes)

## Installation 

Notify is hosted on the [Maven Central Repository](https://mvnrepository.com/artifact/com.dorkbox/Notify) and
can be included in your project by adding it as a dependency in your build file.

### ![Gradle](https://i.imgur.com/qtc6bXq.png?1) Gradle
```
dependencies {
	implementation 'com.dorkbox:Notify:4.5'
}
```

### ![Maven](https://i.imgur.com/2TZzobp.png?1) Maven

```xml
<dependency>
    <groupId>com.dorkbox</groupId>
    <artifactId>Notify</artifactId>
    <version>4.5</version>
</dependency>
```

### ![Scala SBT](https://i.imgur.com/Nqv3mVd.png?1) Scala SBT

```
libraryDependencies += "com.dorkbox" % "Notify" % "4.5"
```

## Basic Usage
```java
Notify.create()
      .title("Title Text")
      .text("Hello World!")
      .useDarkStyle()
      .showWarning();
```

## Features

1. Can specify which screen to use for notification.
2. Can specify which corner (center is also possible) to use for notification 
3. If no location is specified, it will show on whatever screen the mouse is on (if a desktop notification)
4. Duration timeouts, with progress indicator on notification
5. Light or Dark themes
6. Can close via close button or clicking on notification body
7. Can show/hide the close button
8. Can register a callback for when a user clicks on the notification body
9. Animates to a collated position if multiple notifications are in the same position
10. Bypasses the swing EDT, and now renders at a beautiful 30 frames-per-second.
11. Can have notifications in an application or on the desktop.


- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+
- You will need the images in the 'resources' directory, in addition to the normal libs.
- Note: If you want to **COMPLETELY** remove repainting by the swing EDT (for the entire JVM), run `NullRepaintManager.install();`

```
Customization parameters:

-ActiveRenderLoop.TARGET_FPS  (type int, default value '30')
 - How many frames per second we want the Swing ActiveRender thread to run at?
 - NOTE: The ActiveRenderLoop replaces the Swing EDT (only for specified JFrames) in order to enable smoother animations. 
  It is also important to REMEMBER -- if you add a component to an actively managed JFrame, YOU MUST make sure to call
  JComponent.setIgnoreRepaint(boolean) otherwise this component will "fight" on the EDT for updates. You can completely
  disable the EDT by calling NullRepaintManager.install()


Notify.IMAGE_PATH    (type String, default value 'resources')
 - Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
 
 
Notify.TITLE_TEXT_FONT    (type String, default value 'Source Code Pro BOLD 16')
 - This is the title font used by a notification.

 
Notify.MAIN_TEXT_FONT    (type String, default value 'Source Code Pro BOLD 12')
 - This is the main text font used by a notification.
    
 
Notify.MOVE_DURATION    (type float, default value '1.0F')
 - How long we want it to take for the popups to relocate when one is closed
```

![light theme](https://raw.githubusercontent.com/dorkbox/Notify/master/notify-light.png)

![dark theme](https://raw.githubusercontent.com/dorkbox/Notify/master/notify-dark.png)

## Release Notes
It is important to note that notifications for an application use the [glassPane](https://docs.oracle.com/javase/tutorial/uiswing/components/rootpane.html#glasspane) and sets it's ````layoutManager```` to ````null````. This can cause problems with some applications, and you'll need to work around this limitation.