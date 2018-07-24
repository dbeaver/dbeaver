package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.mylyn.commons.notifications.core.AbstractNotification;
import org.eclipse.mylyn.commons.notifications.ui.NotificationsUi;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;

import java.util.Collections;

public abstract class NotificationUtils {

    private static final Log log = Log.getLog(NotificationUtils.class);

    public static void sendNotification(DBPDataSource dataSource, String id, String text) {
        sendNotification(dataSource, id, text, null, null);
    }

    public static void sendNotification(DBPDataSource dataSource, String id, String text, DBPMessageType messageType, Runnable feedback) {
        if (!ModelPreferences.getPreferences().getBoolean(ModelPreferences.NOTIFICATIONS_ENABLED)) {
            return;
        }
        AbstractNotification notification = new DatabaseNotification(
            dataSource,
            id,
            text,
            messageType,
            feedback);
        try {
            NotificationsUi.getService().notify(
                Collections.singletonList(notification));
        } catch (Exception e) {
            log.debug("Error sending Mylin notification", e);
        }
    }

}