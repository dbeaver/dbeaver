package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.mylyn.internal.commons.notifications.ui.popup.NotificationPopup;
import org.eclipse.swt.widgets.Shell;

public class DatabaseNotificationPopup extends NotificationPopup {


    public DatabaseNotificationPopup(Shell parent) {
        super(parent);
        setDelayClose(3000);
    }
}