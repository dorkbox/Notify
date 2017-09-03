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
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.util.ActionHandler;
import dorkbox.util.ScreenUtil;

public
class NotifyTest {

    public static
    void main(String[] args) {
        Notify notify;


        JFrame frame = new JFrame("Test");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        JLabel label = new JLabel("This is a label!");

        JButton button = new JButton();
        button.setText("Press me");

        panel.add(label);
        panel.add(button);

        frame.add(panel);
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        ScreenUtil.showOnSameScreenAsMouse_Center(frame);



        int count = 2;

        for (int i = 0; i < count; i++) {
            final int finalI = i;
            notify = Notify.create()
                           .title("Notify title " + i)
                           .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                                 "notification popup message")
                           .hideAfter(13000)
                           .position(Pos.BOTTOM_RIGHT)
                           // .position(Pos.CENTER)
                      // .setScreen(0)
                           .darkStyle()
                           // .shake(1300, 4)
                      // .shake(1300, 10)
                           .attach(frame)
                      .hideCloseButton()
                           .onAction(new ActionHandler<Notify>() {
                               @Override
                               public
                               void handle(final Notify arg0) {
                                   System.err.println("Notification " + finalI + " clicked on!");
                               }
                           });
            notify.showWarning();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < count; i++) {
            final int finalI = i;
            notify = Notify.create()
                           .title("Notify title " + i)
                           .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                                 "notification popup message")
                           .hideAfter(3000)
                           .position(Pos.TOP_RIGHT)
//                       .setScreen(0)
                           .darkStyle()
                           // .shake(1300, 4)
                      // .shake(1300, 10)
                      .hideCloseButton()
                           .onAction(new ActionHandler<Notify>() {
                               @Override
                               public
                               void handle(final Notify arg0) {
                                   System.err.println("Notification " + finalI + " clicked on!");
                               }
                           });
            notify.showConfirm();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < count; i++) {
            final int finalI = i;
            notify = Notify.create()
                           .title("Notify title " + i)
                           .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                                 "notification popup message")
                           .hideAfter(13000)
                           .position(Pos.BOTTOM_LEFT)
//                       .setScreen(0)
//                            .darkStyle()
                           // .shake(1300, 4)
                           // .shake(1300, 10)
                           .hideCloseButton()
                           .onAction(new ActionHandler<Notify>() {
                               @Override
                               public
                               void handle(final Notify arg0) {
                                   System.err.println("Notification " + finalI + " clicked on!");
                               }
                           });
            notify.show();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
