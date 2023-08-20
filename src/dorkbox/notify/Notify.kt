/*
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.notify

import dorkbox.propertyLoader.Property
import dorkbox.util.ImageUtil
import dorkbox.util.LocationResolver
import dorkbox.util.SwingUtil
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.lang.ref.SoftReference
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame

/**
 * Popup notification messages, similar to the popular "Growl" notification system on macosx, that display in the corner of the monitor.
 *
 * They can follow the mouse (if the screen is unspecified), and have a variety of features, such as "shaking" to draw attention,
 * animating upon movement (for collating w/ multiple in a single location), and automatically hiding after a set duration.
 *
 * These notifications are for a single screen only, and cannot be anchored to an application.
 *
 * ```
 * `Notify()
 * .title("Title Text")
 * .text("Hello World!")
 * .darkStyle()
 * .showWarning()
 * ```
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class Notify private constructor() {
    companion object {
        const val DIALOG_CONFIRM = "dialog-confirm.png"
        const val DIALOG_INFORMATION = "dialog-information.png"
        const val DIALOG_WARNING = "dialog-warning.png"
        const val DIALOG_ERROR = "dialog-error.png"

        /**
         * The width of a notification
         */
        @Property
        var WIDTH = 300

        /**
         * The height of a notification
         */
        @Property
        var HEIGHT = 87

        /**
         * The space between notifications
         */
        @Property
        var SPACER = 10

        /**
         * The space between notifications and the edge of the <screen>/<application border>
         */
        @Property
        var MARGIN = 20

        /**
         * This is the title font used by a notification.
         */
        @Property
        var TITLE_TEXT_FONT = "Source Code Pro BOLD 16"

        /**
         * This is the main text font used by a notification.
         */
        @Property
        var MAIN_TEXT_FONT = "Source Code Pro BOLD 12"

        /**
         * How long we want it to take for the popups to relocate when one is closed
         */
        @Property
        var MOVE_DURATION = 1.0f

        /**
         * Location of the dialog image resources. By default, they must be in the 'resources' directory relative to the application
         */
        @Property
        var IMAGE_PATH = "resources"
        private val imageCache = mutableMapOf<String, SoftReference<ImageIcon>>()

        /**
         * Gets the version number.
         */
        const val version = "4.2"

        init {
            // Add this project to the updates system, which verifies this class + UUID + version information
            dorkbox.updates.Updates.add(Notify::class.java, "8916aaf704e6457ba139cdd501e41797", version)
        }

        /**
         * Builder pattern to create the notification.
         */
        fun create(): Notify {
            return Notify()
        }

        /**
         * Gets the size of the image to be used in the notification, which is a 48x48 pixel image.
         */
        val imageSize: Int
            get() = 48

        /**
         * Permits one to override the default images for the dialogs. This is NOT thread safe, and must be performed BEFORE showing a
         * notification.
         *
         *
         * The image names are as follows:
         *
         *
         * 'Notify.DIALOG_CONFIRM' 'Notify.DIALOG_INFORMATION' 'Notify.DIALOG_WARNING' 'Notify.DIALOG_ERROR'
         *
         * @param imageName  the name of the image, either your own if you want it cached, or one of the above.
         * @param image  the BufferedImage that you want to cache.
         */
        fun overrideDefaultImage(imageName: String, image: BufferedImage) {
            if (imageCache.containsKey(imageName)) {
                throw RuntimeException("Unable to set an image that already has been set. This action must be done as soon as possible.")
            }

            ImageUtil.waitForImageLoad(image)

            // we only use 48x48 pixel images. Resize as necessary
            val width = image.getWidth(null)
            val height = image.getHeight(null)

            // resize the image, keep aspect ratio
            val bufferedImage = if (width > height) {
                ImageUtil.resizeImage(image, imageSize, -1)
            } else {
                ImageUtil.resizeImage(image, -1, imageSize)
            }

            imageCache[imageName] = SoftReference(ImageIcon(bufferedImage))
        }

        private fun getImage(imageName: String): ImageIcon {
            var resourceAsStream: InputStream? = null

            var image = imageCache[imageName]?.get()

            try {
                if (image == null) {
                    // String name = IMAGE_PATH + File.separatorChar + imageName;
                    resourceAsStream = LocationResolver.getResourceAsStream(imageName)
                    image = ImageIcon(ImageIO.read(resourceAsStream))
                    imageCache[imageName] = SoftReference(image)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                resourceAsStream?.close()
            }

            return image!!
        }
    }

    @Volatile
    internal  var notifyPopup: NotifyType<*>? = null

    @Volatile
    var title = "Notification"
        set(value) {
            field = value
            notifyPopup?.refresh()
        }

    @Volatile
    var text = "Lorem ipsum"
        set(value) {
            field = value
            notifyPopup?.refresh()
        }

    @Volatile
    var theme = Theme.defaultLight
        set(value) {
            field = value
            notifyPopup?.refresh()
        }

    @Volatile
    var position = Position.BOTTOM_RIGHT

    @Volatile
    var hideAfterDurationInMillis = 0

    /**
     * Is the close button in the top-right corner of the notification visible
     */
    @Volatile
    var hideCloseButton = false
        set(value) {
            field = value
            notifyPopup?.refresh()
        }

    @Volatile
    var screen = Short.MIN_VALUE.toInt()

    @Volatile
    var image: ImageIcon? = null
        set(value) {
            field = value
            notifyPopup?.refresh()
        }

    /**
     * Called when the notification is closed, either via close button or via close()
     */
    @Volatile
    var onCloseAction: Notify.()->Unit = {}

    /**
     * Called when the "general area" (but specifically not the "close button") is clicked.
     */
    @Volatile
    var onClickAction: Notify.()->Unit = {}

    @Volatile
    var shakeDurationInMillis = 0

    @Volatile
    var shakeAmplitude = 0

    @Volatile
    var attachedFrame: JFrame? = null

    /**
     * Specifies the main text
     */
    fun text(text: String): Notify {
        this.text = text
        return this
    }

    /**
     * Specifies the title
     */
    fun title(title: String): Notify {
        this.title = title
        return this
    }

    /**
     * Specifies the image
     */
    fun image(image: Image): Notify {
        // we only use 48x48 pixel images. Resize as necessary
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        var bufferedImage = ImageUtil.getBufferedImage(image)

        // resize the image, keep aspect ratio
        bufferedImage = if (width > height) {
            ImageUtil.resizeImage(bufferedImage, 48, -1)
        } else {
            ImageUtil.resizeImage(bufferedImage, -1, 48)
        }

        // we have to now clamp to a max dimension of 48
        bufferedImage = ImageUtil.clampMaxImageSize(bufferedImage, 48)

        // now we want to center the image
        bufferedImage = ImageUtil.getSquareBufferedImage(bufferedImage)

        this.image = ImageIcon(bufferedImage)

        return this
    }

    /**
     * Specifies the position of the notification on screen, by default it is [bottom-right][Position.BOTTOM_RIGHT].
     */
    fun position(position: Position): Notify {
        this.position = position
        return this
    }

    /**
     * Specifies the duration that the notification should show, after which it will be hidden. 0 means to show forever. By default it
     * will show forever
     */
    fun hideAfter(durationInMillis: Int): Notify {
        hideAfterDurationInMillis = if (durationInMillis < 0) {
            0
        } else {
            durationInMillis
        }

        return this
    }

    /**
     * Called when the notification is closed, either via close button or via close()
     */
    fun onCloseAction(onAction: Notify.()->Unit): Notify {
        onCloseAction = onAction
        return this
    }

    /**
     * Called when the "general area" (but specifically not the "close button") is clicked.
     */
    fun onClickAction(onAction: Notify.()->Unit): Notify {
        onClickAction = onAction
        return this
    }

    /**
     * Specifies what the theme should be, if other than the default. This will always take precedence over the defaults.
     */
    fun theme(theme: Theme): Notify {
        this.theme = theme
        notifyPopup?.refresh()
        return this
    }

    /**
     * Specify that the close button in the top-right corner of the notification should not be shown.
     */
    fun hideCloseButton(): Notify {
        hideCloseButton = true
        notifyPopup?.refresh()
        return this
    }

    /**
     * Shows the notification with the built-in 'warning' image.
     */
    fun showWarning() {
        image = getImage(DIALOG_WARNING)
        show()
    }

    /**
     * Shows the notification with the built-in 'information' image.
     */
    fun showInformation() {
        image = getImage(DIALOG_INFORMATION)
        show()
    }

    /**
     * Shows the notification with the built-in 'error' image.
     */
    fun showError() {
        image = getImage(DIALOG_ERROR)
        show()
    }

    /**
     * Shows the notification with the built-in 'confirm' image.
     */
    fun showConfirm() {
        image = getImage(DIALOG_CONFIRM)
        show()
    }

    /**
     * Shows the notification. If the Notification is assigned to a screen, but shown inside a Swing/etc parent, the screen number will be
     * ignored.
     */
    fun show() {
        val notify = this@Notify

        // must be done in the swing EDT
        SwingUtil.invokeAndWaitQuietly {
            if (notify.notifyPopup != null) {
                return@invokeAndWaitQuietly
            }

            val window = notify.attachedFrame
            val shakeDuration = notify.shakeDurationInMillis
            val shakeAmp = notify.shakeAmplitude

            val notifyPopup = if (window == null) {
                DesktopNotify(notify)
            } else {
                AppNotify(notify)
            }

            notifyPopup.setVisible(true)

            if (shakeDuration > 0) {
                notifyPopup.shake(shakeDuration, shakeAmp)
            }

            notify.notifyPopup = notifyPopup
        }
    }

    /**
     * "Shakes" the notification, to bring user attention to it.
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    fun shake(durationInMillis: Int = 2000, amplitude: Int = 4): Notify {
        shakeDurationInMillis = durationInMillis
        shakeAmplitude = amplitude

        val popup = notifyPopup
        if (popup !== null) {
            // must be done in the swing EDT
            SwingUtil.invokeLater { popup.shake(durationInMillis, amplitude) }
        }
        return this
    }

    /**
     * Closes the notification. Particularly useful if it's an "infinite" duration notification.
     */
    fun close() {
        val popup = notifyPopup
        if (popup !== null) {
            // must be done in the swing EDT
            SwingUtil.invokeLater {
                popup.close()
            }
        }
    }

    /**
     * Specifies which screen to display on. If <0, it will show on screen 0. If > max-screens, it will show on the last screen.
     */
    fun setScreen(screenNumber: Int): Notify {
        this.screen = screenNumber
        return this
    }

    /**
     * Attaches this notification to a specific JFrame, instead of having a global notification
     */
    fun attach(frame: JFrame?): Notify {
        this.attachedFrame = frame
        return this
    }




    internal fun onClose() {
        // we can close in different ways.
        // 1) via the close button
        // 2) expiration of the tween
        // 3) manually closing the notification
        // all events arrive via the active renderer event queue, so effectively single threaded

        if (notifyPopup != null) {
            this.notifyPopup!!.close()

            // we want the event dispatched on the Swing EDT. This is called by the active renderer
            SwingUtil.invokeLater {
                this.onCloseAction.invoke(this)
            }
        }

        notifyPopup = null
    }

    internal fun onClickAction() {
        // we want the event dispatched on the Swing EDT. This is called by the active renderer
        SwingUtil.invokeLater {
            this.onClickAction.invoke(this)
        }
    }
}
