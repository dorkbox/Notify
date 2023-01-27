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
 * <pre>
 * `Notify.create()
 * .title("Title Text")
 * .text("Hello World!")
 * .useDarkStyle()
 * .showWarning();
` *
</pre> *
 */
@Suppress("unused")
class Notify private constructor() {
    companion object {
        const val DIALOG_CONFIRM = "dialog-confirm.png"
        const val DIALOG_INFORMATION = "dialog-information.png"
        const val DIALOG_WARNING = "dialog-warning.png"
        const val DIALOG_ERROR = "dialog-error.png"

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
         * Location of the dialog image resources. By default they must be in the 'resources' directory relative to the application
         */
        @Property
        var IMAGE_PATH = "resources"
        private val imageCache = mutableMapOf<String, SoftReference<ImageIcon>>()

        /**
         * Gets the version number.
         */
        const val version = "3.7"

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

        private fun getImage(imageName: String): ImageIcon? {
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

            return image
        }
    }


    internal var title = "Notification"
    internal var text = "Lorem ipsum"
    private var theme: Theme? = null
    internal var position = Pos.BOTTOM_RIGHT
    internal var hideAfterDurationInMillis = 0
    internal var hideCloseButton = false
    private var isDark = false
    internal var screenNumber = Short.MIN_VALUE.toInt()

    private var icon: ImageIcon? = null
    internal var onGeneralAreaClickAction: Notify.()->Unit = {}

    private var notifyPopup: INotify? = null
    private var name: String? = null
    private var shakeDurationInMillis = 0
    private var shakeAmplitude = 0
    private var appWindow: JFrame? = null

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
        icon = ImageIcon(bufferedImage)
        return this
    }

    /**
     * Specifies the position of the notification on screen, by default it is [bottom-right][Pos.BOTTOM_RIGHT].
     */
    fun position(position: Pos): Notify {
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
     * Specifies what to do when the user clicks on the notification (in addition o the notification hiding, which happens whenever the
     * notification is clicked on). This does not apply when clicking on the "close" button
     */
    fun onAction(onAction: Notify.()->Unit): Notify {
        onGeneralAreaClickAction = onAction
        return this
    }

    /**
     * Specifies that the notification should use the built-in dark styling, rather than the default, light-gray notification style.
     */
    fun darkStyle(): Notify {
        isDark = true
        return this
    }

    /**
     * Specifies what the theme should be, if other than the default. This will always take precedence over the defaults.
     */
    fun text(theme: Theme?): Notify {
        this.theme = theme
        return this
    }

    /**
     * Specify that the close button in the top-right corner of the notification should not be shown.
     */
    fun hideCloseButton(): Notify {
        hideCloseButton = true
        return this
    }

    /**
     * Shows the notification with the built-in 'warning' image.
     */
    fun showWarning() {
        name = DIALOG_WARNING
        icon = getImage(DIALOG_WARNING)
        show()
    }

    /**
     * Shows the notification with the built-in 'information' image.
     */
    fun showInformation() {
        name = DIALOG_INFORMATION
        icon = getImage(DIALOG_INFORMATION)
        show()
    }

    /**
     * Shows the notification with the built-in 'error' image.
     */
    fun showError() {
        name = DIALOG_ERROR
        icon = getImage(DIALOG_ERROR)
        show()
    }

    /**
     * Shows the notification with the built-in 'confirm' image.
     */
    fun showConfirm() {
        name = DIALOG_CONFIRM
        icon = getImage(DIALOG_CONFIRM)
        show()
    }

    /**
     * Shows the notification. If the Notification is assigned to a screen, but shown inside a Swing/etc parent, the screen number will be
     * ignored.
     */
    fun show() {
        // must be done in the swing EDT
        SwingUtil.invokeAndWaitQuietly {
            val notify = this@Notify
            val image = notify.icon
            val theme = if (notify.theme != null) {
                // use custom provided theme
                notify.theme!!
            } else {
                Theme(TITLE_TEXT_FONT, MAIN_TEXT_FONT, notify.isDark)
            }
            val window = appWindow

            val notifyPopup = if (window == null) {
                AsDesktop(notify, image, theme)
            } else {
                AsApplication(notify, image, window, theme)
            }

            notifyPopup.setVisible(true)

            if (shakeDurationInMillis > 0) {
                notifyPopup.shake(notify.shakeDurationInMillis, notify.shakeAmplitude)
            }

            notify.notifyPopup = notifyPopup
        }

        // don't need to hang onto these.
        icon = null
    }

    /**
     * "shakes" the notification, to bring user attention to it.
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    fun shake(durationInMillis: Int, amplitude: Int): Notify {
        shakeDurationInMillis = durationInMillis
        shakeAmplitude = amplitude
        if (notifyPopup != null) {
            // must be done in the swing EDT
            SwingUtil.invokeLater { notifyPopup!!.shake(durationInMillis, amplitude) }
        }
        return this
    }

    /**
     * Closes the notification. Particularly useful if it's an "infinite" duration notification.
     */
    fun close() {
        if (notifyPopup == null) {
            throw NullPointerException("NotifyPopup")
        }

        // must be done in the swing EDT
        SwingUtil.invokeLater { notifyPopup!!.close() }
    }

    /**
     * Specifies which screen to display on. If <0, it will show on screen 0. If > max-screens, it will show on the last screen.
     */
    fun setScreen(screenNumber: Int): Notify {
        this.screenNumber = screenNumber
        return this
    }

    /**
     * Attaches this notification to a specific JFrame, instead of having a global notification
     */
    fun attach(frame: JFrame?): Notify {
        appWindow = frame
        return this
    }

    // called when this notification is closed.
    fun onClose() {
        notifyPopup = null
    }


}
