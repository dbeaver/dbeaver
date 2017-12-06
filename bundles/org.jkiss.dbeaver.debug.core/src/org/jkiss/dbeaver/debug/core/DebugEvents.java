package org.jkiss.dbeaver.debug.core;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;

public class DebugEvents {
    
    /**
     * Fires the given debug event.
     *
     * @param event debug event to fire
     */
    public static void fireEvent(DebugEvent event) {
        DebugPlugin manager= DebugPlugin.getDefault();
        if (manager != null) {
            manager.fireDebugEventSet(new DebugEvent[]{event});
        }
    }

    /**
     * Fires a terminate event.
     */
    public static void fireTerminate(Object source) {
        fireEvent(new DebugEvent(source, DebugEvent.TERMINATE));
    }

}
