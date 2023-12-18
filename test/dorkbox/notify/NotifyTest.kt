/*
 * Copyright 2023 dorkbox, llc
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

import dorkbox.util.ImageUtil
import dorkbox.util.LocationResolver
import dorkbox.util.ScreenUtil
import java.awt.FlowLayout
import java.awt.Image
import java.io.IOException
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel



object NotifyTest {
    @JvmStatic
    fun main(args: Array<String>) {
//        SwingActiveRender.TARGET_FPS = 60

        val frame = JFrame("Test")
        val panel = JPanel()
        panel.layout = FlowLayout()

        val label = JLabel("This is a label!")

        val buttonApp = JButton()
        buttonApp.text = "Press me App"
        buttonApp.addActionListener {
            println("Clicked button App!")
            manageApp(frame)
        }

        val buttonScreen = JButton()
        buttonScreen.text = "Press me Screen"
        buttonScreen.addActionListener {
            println("Clicked button Screen!")
            manageScreen(frame)
        }

        panel.add(label)
        panel.add(buttonApp)
        panel.add(buttonScreen)
        frame.add(panel)
        frame.setSize(900, 600)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true

        ScreenUtil.showOnSameScreenAsMouse_Center(frame)

        manageApp(frame)
        manageScreen(frame)
    }

    private val notifyCount = AtomicInteger(0)

    fun manageApp(frame: JFrame) {
        timerApp(frame, Position.BOTTOM, TimeUnit.SECONDS.toMillis(20))
    }

    fun manageScreen(frame: JFrame) {
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

        image!!

        screen(image, Position.BOTTOM, TimeUnit.SECONDS.toMillis(20))
    }


    private fun screen(image: Image, position: Position, durationMs: Long = 0) {
        val count = notifyCount.getAndIncrement()

        val notify = Notify.create()
             .title("Notify title $count")
             .text("""This is a notification $count popup message This is a notification popup message This is a notification popup message""")
             .hideAfter(durationMs)
             .position(position)
            // .setScreen(0)
            // .darkStyle()
             .theme(Theme.defaultDark)
            // .shake(1300, 4)
            // .shake(1300, 10)
            // .hideCloseButton()
             .onClickAction {
                 text = "HI: $count"
                 System.err.println("Notification $count clicked on!")
             }

        notify.image(image)
        notify.show()
//            notify.showConfirm()
    }

    fun timerApp(frame: JFrame, position: Position, durationMs: Long = 0) {
        val count = notifyCount.getAndIncrement()

        val notify = Notify.create()
            .title("Notify title $count")
            .text("This is a notification $count popup message This is a notification popup message This is a notification popup message")
            .hideAfter(durationMs)
            .position(position)
            // .position(Pos.CENTER)
            // .setScreen(0)
            .theme(Theme.defaultDark)
            // .shake(1300, 4)
//            .shake(13200, 10)
            .attach(frame)

            .onClickAction {
                text = "HI: $count"
                System.err.println("Notification $count clicked on!")
            }
//                    .hideCloseButton() // if the hideButton is visible, then it's possible to change things when clicked
            .attach(frame)
        notify.show()
//        notify.showWarning()
    }
}
