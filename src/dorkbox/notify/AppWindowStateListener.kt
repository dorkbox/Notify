package dorkbox.notify

import java.awt.Frame
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener

internal class AppWindowStateListener(val notify: AppNotify): WindowStateListener {
    override fun windowStateChanged(e: WindowEvent) {
        val state = e.newState
        if (state and Frame.ICONIFIED == 0) {
            notify.reLayout()
        }
    }
}
