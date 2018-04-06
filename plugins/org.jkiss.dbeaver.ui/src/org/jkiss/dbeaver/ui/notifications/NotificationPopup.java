package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.utils.GeneralUtils;

public abstract class NotificationPopup extends AbstractNotificationPopup {

    public NotificationPopup(Display display) {
        super(display);
    }

    public NotificationPopup(Display display, int style) {
        super(display, style);
    }



}