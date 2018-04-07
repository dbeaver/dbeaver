package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.mylyn.commons.notifications.ui.AbstractUiNotification;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;

import java.util.Date;

public class DatabaseNotification extends AbstractUiNotification {

    private final String label;
    private final String description;
    private final DBPMessageType messageType;
    private final Runnable feedback;
    private final Date date;

    public DatabaseNotification(
        @Nullable DBPDataSource dataSource,
        @NotNull String id,
        @NotNull String description,
        @Nullable DBPMessageType messageType,
        @Nullable Runnable feedback)
    {
        super("org.jkiss.dbeaver.notifications.event." + id);
        this.label = dataSource.getContainer().getName();
        this.description = description;
        this.messageType = messageType;
        this.feedback = feedback;
        this.date = new Date();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Date getDate() {
        return date;
    }


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public Image getNotificationImage() {
        return null;
    }

    @Override
    public Image getNotificationKindImage() {
        if (messageType == null) {
            return null;
        }
        int iconType;
        switch (messageType) {
            case ERROR: iconType = SWT.ICON_ERROR; break;
            case WARNING: iconType = SWT.ICON_WARNING; break;
            default: iconType = SWT.ICON_INFORMATION; break;
        }
        return Display.getDefault().getSystemImage(iconType);
    }

    @Override
    public void open() {
        if (feedback != null) {
            feedback.run();
        }
    }

}