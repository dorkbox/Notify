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
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.util.ActionHandler;

public
class NotifyTest {

    public static
    void main(String[] args) {
        Notify notify;

        int count = 3;

        for (int i = 0; i < count; i++) {
            notify = Notify.create()
                           .title("Notify title " + i)
                           .text("This is a notification " + i + " popup message This is a notification popup message This is a " +
                                 "notification popup message")
                           .hideAfter(50000)
                           .position(Pos.TOP_RIGHT)
//                       .setScreen(0)
                           .darkStyle()
                           .shake(1300, 4)
//                       .shake(1300, 10)
//                       .hideCloseButton()
                           .onAction(new ActionHandler<Notify>() {
                                   @Override
                                   public
                                   void handle(final Notify arg0) {
                                       System.out.println("Notification clicked on!");
                                   }
                               });
            notify.showWarning();
        }
    }
}
