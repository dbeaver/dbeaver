package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class NotificationPopupMessage extends DatabaseNotificationPopup {

    private final DBPDataSource dataSource;
    private String messageText;
    private int iconType;

    public NotificationPopupMessage(DBPDataSource dataSource, String text, int iconType) {
        super(PlatformUI.getWorkbench().getDisplay().getActiveShell());

        this.dataSource = dataSource;
        this.messageText = text;
        this.iconType = iconType;
    }

    @Override
    protected String getPopupShellTitle() {
        return dataSource == null ? GeneralUtils.getProductName() : dataSource.getContainer().getName();
    }

    @Override
    protected Image getPopupShellImage(int maximumHeight) {
        boolean hasIcon = iconType == SWT.ICON_ERROR || iconType == SWT.ICON_WARNING || iconType == SWT.ICON_QUESTION;
        return hasIcon ? getShell().getDisplay().getSystemImage(iconType) : null;
    }

    @Override
    protected void createContentArea(Composite composite)
    {
        Label textLabel = new Label(composite, SWT.NONE);
        textLabel.setText(messageText);
    }

    public static void showMessage(DBPDataSource dataSource, String text, long delayClose, int iconType) {
        Display.getDefault().syncExec(() -> {
            NotificationPopupMessage popup = new NotificationPopupMessage(dataSource, text, iconType);
            if (delayClose > 0) {
                popup.setDelayClose(delayClose);
            }

            popup.open();
        });
    }

}