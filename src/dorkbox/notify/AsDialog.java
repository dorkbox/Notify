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
package dorkbox.notify;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;

import dorkbox.util.SwingUtil;

// this is a child to a Jframe/window (instead of globally to the screen)
@SuppressWarnings({"Duplicates", "FieldCanBeLocal", "WeakerAccess", "DanglingJavadoc"})
public
class AsDialog extends JDialog implements INotify {
    private static final long serialVersionUID = 1L;

    private final LookAndFeel look;
    private final Notify notification;
    private final ComponentListener parentListener;

    // this is on the swing EDT
    @SuppressWarnings("NumericCastThatLosesPrecision")
    AsDialog(final Notify notification, final ImageIcon image, final Window container, final Theme theme) {
        super(container, Dialog.ModalityType.MODELESS);
        this.notification = notification;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        final Dimension preferredSize = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(preferredSize);
        setMaximumSize(preferredSize);
        setMinimumSize(preferredSize);
        setSize(NotifyCanvas.WIDTH, NotifyCanvas.HEIGHT);
        setLocation(Short.MIN_VALUE, Short.MIN_VALUE);

        setTitle(notification.title);
        setResizable(false);

        NotifyCanvas notifyCanvas = new NotifyCanvas(notification, image, theme);
        add(notifyCanvas);

        look = new LookAndFeel(this, notifyCanvas, notification, image, container.getBounds());

        // this makes sure that our dialogs follow the parent window (if it's hidden/shown/moved/etc)
        parentListener = new ComponentListener() {
            @Override
            public
            void componentShown(final ComponentEvent e) {
                AsDialog.this.setVisible(true);
                look.reLayout(container.getBounds());
            }

            @Override
            public
            void componentHidden(final ComponentEvent e) {
                AsDialog.this.setVisible(false);
            }

            @Override
            public
            void componentResized(final ComponentEvent e) {
                look.reLayout(container.getBounds());
            }

            @Override
            public
            void componentMoved(final ComponentEvent e) {
                look.reLayout(container.getBounds());
            }
        };

        container.addWindowStateListener(new WindowStateListener() {
            @Override
            public
            void windowStateChanged(WindowEvent e) {
                int state = e.getNewState();
                if ((state & Frame.ICONIFIED) != 0) {
                    setVisible(false);
                }
                else {
                    setVisible(true);
                    look.reLayout(container.getBounds());
                }
            }
        });

        container.addComponentListener(parentListener);
    }

    @Override
    public
    void onClick(final int x, final int y) {
        look.onClick(x, y);
    }

    /**
     * Shakes the popup
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    @Override
    public
    void shake(final int durationInMillis, final int amplitude) {
        look.shake(durationInMillis, amplitude);
    }

    @Override
    public
    void setVisible(final boolean visible) {
        // was it already visible?
        if (visible == isVisible()) {
            // prevent "double setting" visible state
            return;
        }

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPre(visible);

        super.setVisible(visible);

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPost(visible);
    }

    @Override
    public
    void close() {
        // this must happen in the Swing EDT. This is usually called by the active renderer
        SwingUtil.invokeLater(new Runnable() {
            @Override
            public
            void run() {
                // set it off screen (which is what the close method also does)
                if (isVisible()) {
                    setVisible(false);
                }

                look.close();

                if (parentListener != null) {
                    removeComponentListener(parentListener);
                }
                setIconImage(null);
                removeAll();
                dispose();

                notification.onClose();
            }
        });
    }
}
