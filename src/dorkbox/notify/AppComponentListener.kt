package dorkbox.notify

import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener

internal class AppComponentListener(val notify: AppNotify) : ComponentListener {
    override fun componentShown(e: ComponentEvent) {
        notify.reLayout()
    }

    override fun componentHidden(e: ComponentEvent) {}

    override fun componentResized(e: ComponentEvent) {
        notify.reLayout()
    }

    override fun componentMoved(e: ComponentEvent) {}
}
