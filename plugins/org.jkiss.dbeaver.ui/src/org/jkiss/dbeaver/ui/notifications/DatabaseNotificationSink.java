package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.notifications.core.AbstractNotification;
import org.eclipse.mylyn.commons.notifications.core.NotificationSink;
import org.eclipse.mylyn.commons.notifications.core.NotificationSinkEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ModelPreferences;

import java.util.*;

public class DatabaseNotificationSink extends NotificationSink {

    private static final long DELAY_OPEN = 200;

    private static final boolean runSystem = true;

    private final WeakHashMap<Object, Object> cancelledTokens = new WeakHashMap<>();
    private final Set<AbstractNotification> notifications = new HashSet<>();
    private final Set<AbstractNotification> currentlyNotifying = Collections.synchronizedSet(notifications);

    private final Job openJob = new Job("Database notifications") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                if (Platform.isRunning() && PlatformUI.getWorkbench() != null
                    && PlatformUI.getWorkbench().getDisplay() != null
                    && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
                    PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                        collectNotifications();

                        if (popup != null && popup.getReturnCode() == Window.CANCEL) {
                            List<AbstractNotification> notifications = popup.getNotifications();
                            for (AbstractNotification notification : notifications) {
                                if (notification.getToken() != null) {
                                    cancelledTokens.put(notification.getToken(), null);
                                }
                            }
                        }

                        currentlyNotifying.removeIf(notification -> notification.getToken() != null
                            && cancelledTokens.containsKey(notification.getToken()));

                        synchronized (DatabaseNotificationSink.class) {
                            if (currentlyNotifying.size() > 0) {
//										popup.close();
                                showPopup();
                            }
                        }
                    });
                }
            } finally {
                if (popup != null) {
                    schedule(popup.getDelayClose() / 2);
                }
            }

            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            return Status.OK_STATUS;
        }

    };

    private DatabaseNotificationPopup popup;

    public DatabaseNotificationSink() {
        openJob.setSystem(runSystem);
    }

    private void cleanNotified() {
        currentlyNotifying.clear();
    }

    /** public for testing */
    private void collectNotifications() {
    }

    /**
     * public for testing purposes
     */
    public Set<AbstractNotification> getNotifications() {
        synchronized (DatabaseNotificationSink.class) {
            return currentlyNotifying;
        }
    }

    private boolean isAnimationsEnabled() {
        IPreferenceStore store = PlatformUI.getPreferenceStore();
        return store.getBoolean(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS);
    }

    @Override
    public void notify(NotificationSinkEvent event) {
        currentlyNotifying.addAll(event.getNotifications());

        if (!openJob.cancel()) {
            try {
                openJob.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        openJob.schedule(DELAY_OPEN);
    }

    private void showPopup() {
        if (popup != null) {
            popup.close();
        }

        Shell shell = new Shell(PlatformUI.getWorkbench().getDisplay());
        popup = new DatabaseNotificationPopup(shell);
        popup.setFadingEnabled(isAnimationsEnabled());

        popup.setDelayClose(ModelPreferences.getPreferences().getInt(ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT));

        List<AbstractNotification> toDisplay = new ArrayList<>(currentlyNotifying);
        Collections.sort(toDisplay);
        popup.setContents(toDisplay);
        cleanNotified();
        popup.setBlockOnOpen(false);

        popup.open();
    }

}