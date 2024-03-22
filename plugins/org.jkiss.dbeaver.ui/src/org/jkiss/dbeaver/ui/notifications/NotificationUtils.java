/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.notifications.sounds.BeepSoundProvider;
import org.jkiss.dbeaver.ui.notifications.sounds.FileSoundProvider;
import org.jkiss.dbeaver.ui.registry.NotificationDescriptor;
import org.jkiss.dbeaver.ui.registry.NotificationRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Collections;
import java.util.function.Supplier;

public abstract class NotificationUtils {

    private static final Log log = Log.getLog(NotificationUtils.class);

    private static final DatabaseNotificationSink NOTIFICATION_SINK = new DatabaseNotificationSink();

    private static final String NOTIFICATIONS_SETTINGS_PREFIX = "notifications.settings."; //$NON-NLS-1$
    private static final String NOTIFICATIONS_KEY_ENABLE_POPUP = ".enablePopup"; //$NON-NLS-1$
    private static final String NOTIFICATIONS_KEY_ENABLE_SOUND = ".enableSound"; //$NON-NLS-1$
    private static final String NOTIFICATIONS_KEY_SOUND_FILE = ".soundFile"; //$NON-NLS-1$

    public static void sendNotification(DBPDataSource dataSource, String id, String text, DBPMessageType messageType, Runnable feedback) {
        sendNotification(id, () -> new DatabaseNotification(dataSource, id, text, messageType, feedback));
    }

    public static void sendNotification(String id, String title, String text, DBPMessageType messageType, Runnable feedback) {
        sendNotification(id, () -> new GeneralNotification(id, title, text, messageType, feedback));
    }

    private static void sendNotification(@NotNull String id, @NotNull Supplier<AbstractNotification> notificationSupplier) {
        if (isPopupEnabled(id)) {
            try {
                NOTIFICATION_SINK.notify(new NotificationSinkEvent(Collections.singletonList(notificationSupplier.get())));
            } catch (Exception e) {
                log.debug("Error sending Mylin notification", e);
            }
        }

        if (isSoundEnabled(id) && DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getBoolean(ModelPreferences.NOTIFICATIONS_SOUND_ENABLED)) {
            final NotificationSoundProvider soundProvider = getNotificationSoundProvider(id);
            if (soundProvider != null) {
                scheduleNotificationSound(soundProvider);
            }
        }
    }

    @NotNull
    public static NotificationSettings getNotificationSettings(@NotNull String id) {
        final NotificationDescriptor notification = NotificationRegistry.getInstance().getNotification(id);

        if (notification == null) {
            throw new IllegalArgumentException("Can't find notification '" + id + "'");
        }

        final String enablePopupKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_ENABLE_POPUP;
        final String enableSoundKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_ENABLE_SOUND;
        final String soundFileKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_SOUND_FILE;

        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        preferences.setDefault(enablePopupKey, true);
        preferences.setDefault(enableSoundKey, notification.isSoundEnabled());

        final NotificationSettings settings = new NotificationSettings();
        settings.setShowPopup(preferences.getBoolean(enablePopupKey));
        settings.setPlaySound(preferences.getBoolean(enableSoundKey));
        settings.setSoundFile(CommonUtils.isEmpty(preferences.getString(soundFileKey)) ? null : new File(preferences.getString(soundFileKey)));

        return settings;
    }

    public static void setNotificationSettings(@NotNull String id, @NotNull NotificationSettings settings) {
        final NotificationDescriptor notification = NotificationRegistry.getInstance().getNotification(id);

        if (notification == null) {
            throw new IllegalArgumentException("Can't find notification '" + id + "'");
        }

        final String enablePopupKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_ENABLE_POPUP;
        final String enableSoundKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_ENABLE_SOUND;
        final String soundFileKey = NOTIFICATIONS_SETTINGS_PREFIX + id + NOTIFICATIONS_KEY_SOUND_FILE;

        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        preferences.setDefault(enablePopupKey, true);
        preferences.setDefault(enableSoundKey, notification.isSoundEnabled());

        preferences.setValue(enablePopupKey, settings.isShowPopup());
        preferences.setValue(enableSoundKey, settings.isPlaySound());
        preferences.setValue(soundFileKey, settings.getSoundFile() == null ? "" : settings.getSoundFile().toString());
    }

    private static boolean isPopupEnabled(@NotNull String id) {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.NOTIFICATIONS_ENABLED)
            && getNotificationSettings(id).isShowPopup();
    }

    private static boolean isSoundEnabled(@NotNull String id) {
        return DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.NOTIFICATIONS_ENABLED)
            && getNotificationSettings(id).isPlaySound();
    }

    @Nullable
    private static NotificationSoundProvider getNotificationSoundProvider(@NotNull String id) {
        final NotificationSettings settings = NotificationUtils.getNotificationSettings(id);

        if (!settings.isPlaySound()) {
            return null;
        } else if (settings.getSoundFile() == null) {
            return BeepSoundProvider.INSTANCE;
        } else {
            return new FileSoundProvider(settings.getSoundFile());
        }
    }

    private static void scheduleNotificationSound(@NotNull NotificationSoundProvider provider) {
        final NotificationSound sound;

        try {
            sound = provider.create();
        } catch (DBException e) {
            log.debug("Unable to play notification sound", e);
            return;
        }

        final AbstractJob job = new AbstractJob("Play notification sound") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                sound.play(DBWorkbench.getPlatform()
                    .getPreferenceStore()
                    .getFloat(ModelPreferences.NOTIFICATIONS_SOUND_VOLUME) / 100.0f);
                return Status.OK_STATUS;
            }
        };
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                sound.close();
            }
        });
        job.setUser(false);
        job.setSystem(true);
        job.schedule();
    }
}