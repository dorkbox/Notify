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
package dorkbox.util.growl;

import javax.swing.Box;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class GrowlCloseAdapter extends MouseAdapter {
    private GrowlPopup growlPopup;
    private Color foreground;

    public
    GrowlCloseAdapter(final GrowlPopup growlPopup) {
        this.growlPopup = growlPopup;
    }

    @Override
    public
    void mouseEntered(final MouseEvent e) {
        Box source = (Box) e.getSource();
        JLabel label = (JLabel) source.getComponent(0);
        foreground = label.getForeground();
        label.setForeground(Color.RED);
    }

    @Override
    public
    void mouseExited(final MouseEvent e) {
        Box source = (Box) e.getSource();
        JLabel label = (JLabel) source.getComponent(0);
        label.setForeground(foreground);
    }

    @Override
    public
    void mouseReleased(final MouseEvent e) {
        growlPopup.close();
    }
}
