@file:Suppress("UNUSED_VALUE")

package dorkbox.notify

import dorkbox.util.ImageUtil
import dorkbox.util.LocationResolver
import dorkbox.util.ScreenUtil
import java.awt.FlowLayout
import java.awt.Image
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

object NotifyTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val frame = JFrame("Test")
        val panel = JPanel()
        panel.layout = FlowLayout()
        val label = JLabel("This is a label!")
        val button = JButton()
        button.text = "Press me"
        button.addActionListener { println("Clicked button!") }

        panel.add(label)
        panel.add(button)
        frame.add(panel)
        frame.setSize(900, 600)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true

        ScreenUtil.showOnSameScreenAsMouse_Center(frame)

        // The purpose of this, is to display this image AS A SQUARE!
        val resourceAsStream = LocationResolver.getResourceAsStream("notify-dark.png")
        var image: Image? = null
        try {
            image = ImageIO.read(resourceAsStream)
            ImageUtil.waitForImageLoad(image)

            // image = image.getScaledInstance(144, 104, Image.SCALE_SMOOTH);
            // image = image.getScaledInstance(104, 144, Image.SCALE_SMOOTH);
            image = image.getScaledInstance(144, 144, Image.SCALE_SMOOTH)
        } catch (e: IOException) {
            e.printStackTrace()
        }

//        bottomRightInFrame(3, frame)
//        topLeftInFrame(3, frame)

        react()
//        topRightMonitor(3)
//        bottomLeftScaled(3, frame, image)
//        bottomLeftStacking(3, frame, image)
    }

    fun react() {
        val notify = Notify.create()
        notify.title("Notify title modify")
                    .text("This is a notification popup message This is a notification popup message This is a " +
                            "notification popup message")
                    .hideAfter(13000)
                    .position(Position.TOP_RIGHT)
                    // .setScreen(0)
                    .theme(Theme.defaultDark)
                    // .shake(1300, 4)
                    .shake(4300, 10)
//                    .hideCloseButton() // if the hideButton is visible, then it's possible to change things when clicked
                    .onClickAction {
                        notify.text = "HOWDY"
                        System.err.println("Notification clicked on!")
                    }
            notify.show()
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
    }

    fun topRightMonitor(count: Int) {
        var notify: Notify

        for (i in 0 until count) {
            notify = Notify.create()
                    .title("Notify title $i")
                    .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                            "notification popup message")
                    .hideAfter(13000)
                    .position(Position.TOP_RIGHT)
                    // .setScreen(0)
                .theme(Theme.defaultDark)
                    // .shake(1300, 4)
                    .shake(4300, 10)
                    .hideCloseButton()
                    .onClickAction { System.err.println("Notification $i clicked on!") }
            notify.show()
            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun bottomLeftScaled(image: Image) {
       val notify = Notify.create()
                .title("Notify scaled")
                .text("This is a notification popup message scaled This is a notification popup message This is a " +
                        "notification popup message scaled ") // .hideAfter(13000)
                .position(Position.BOTTOM_LEFT) //                       .setScreen(0)
                //                            .darkStyle()
                // .shake(1300, 4)
                // .shake(1300, 10)
                // .hideCloseButton()
                .onClickAction { System.err.println("Notification scaled clicked on!") }
        notify.image(image)
        notify.show()
    }

    fun bottomLeftStacking(count: Int, image: Image) {
        var notify: Notify

        for (i in 0 until count) {
            notify = Notify.create()
                    .title("Notify title $i")
                    .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                            "notification popup message")
                    // .hideAfter(13000)
                    .position(Position.BOTTOM_LEFT)
                    // .setScreen(0)
                    // .darkStyle()
                    // .shake(1300, 4)
                    // .shake(1300, 10)
                    // .hideCloseButton()
                    .onClickAction { System.err.println("Notification $i clicked on!") }
            if (i == 0) {
                notify.image(image)
                notify.show()
            } else {
                notify.showConfirm()
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun topLeftInFrame(count: Int, frame: JFrame) {
        var notify: Notify
        for (i in 0 until count) {
            notify = Notify.create()
                .title("Notify title $i")
                .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                              "notification popup message")
                .hideAfter(13000)
                .position(Position.TOP_LEFT) // .position(Pos.CENTER)
                // .setScreen(0)
                //      .darkStyle()
                // .shake(1300, 4)
                // .shake(1300, 10)
                .attach(frame) // .hideCloseButton()
                .onClickAction { System.err.println("Notification $i clicked on!") }
            notify.showWarning()

            try {
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun bottomRightInFrame(count: Int, frame: JFrame) {
        var notify: Notify
        for (i in 0 until count) {
            notify = Notify.create()
                .title("Notify title $i")
                .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                              "notification popup message")
                .hideAfter(13000)
                .position(Position.BOTTOM_RIGHT) // .position(Pos.CENTER)
                // .setScreen(0)
                .theme(Theme.defaultDark)
                // .shake(1300, 4)
                .shake(1300, 10)
                .attach(frame)
                .hideCloseButton()
                .onClickAction { System.err.println("Notification $i clicked on!") }
            notify.showWarning()

            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}
