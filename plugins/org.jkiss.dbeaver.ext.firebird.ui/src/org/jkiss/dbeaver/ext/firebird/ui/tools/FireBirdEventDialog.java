/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.ui.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdDataSource;
import org.jkiss.dbeaver.ext.firebird.ui.internal.FireBirdUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverLoader;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FireBirdEventDialog extends BaseDialog {

    private static final Log log = Log.getLog(FireBirdEventDialog.class);

    private final FireBirdDataSource dataSource;

    private Text eventsText;
    private Button startButton;
    private Button stopButton;
    private Text logText;

    private Object eventManager;
    private Object listenerProxy;
    private Class<?> eventListenerClass;
    private final List<String> activeEvents = new ArrayList<>();

    public FireBirdEventDialog(@NotNull Shell parentShell, @NotNull FireBirdDataSource dataSource) {
        super(parentShell, FireBirdUIMessages.dialog_events_title, dataSource.getContainer().getDriver().getIcon());
        this.dataSource = dataSource;
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.MODELESS);
    }

    @NotNull
    @Override
    protected Control createDialogArea(@NotNull Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        // Configuration Group
        Group configGroup = UIUtils.createControlGroup(
            composite,
            FireBirdUIMessages.dialog_events_group_config,
            3,
            GridData.FILL_HORIZONTAL,
            0
        );

        UIUtils.createControlLabel(configGroup, FireBirdUIMessages.dialog_events_label_events);

        eventsText = new Text(configGroup, SWT.BORDER);
        eventsText.setToolTipText(FireBirdUIMessages.dialog_events_label_events_tip);
        eventsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite btnComposite = UIUtils.createComposite(configGroup, 2);
        startButton = UIUtils.createPushButton(btnComposite, FireBirdUIMessages.dialog_events_button_start, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startListening();
            }
        });
        stopButton = UIUtils.createPushButton(btnComposite, FireBirdUIMessages.dialog_events_button_stop, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                stopListening();
            }
        });
        stopButton.setEnabled(false);

        // Log Group
        Group logGroup = UIUtils.createControlGroup(
            composite,
            FireBirdUIMessages.dialog_events_group_log,
            1,
            GridData.FILL_BOTH,
            0
        );

        logText = new Text(logGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        logText.setEditable(false);
        Font monoFont = UIUtils.getMonospaceFont();
        logText.setFont(monoFont);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 650;
        logText.setLayoutData(gd);

        Composite logBtnComposite = UIUtils.createComposite(logGroup, 1);
        logBtnComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        UIUtils.createPushButton(logBtnComposite, FireBirdUIMessages.dialog_events_button_clear, null, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                logText.setText("");
            }
        });

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CLOSE_ID) {
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void startListening() {
        String rawEvents = eventsText.getText().trim();
        if (CommonUtils.isEmpty(rawEvents)) {
            DBWorkbench.getPlatformUI().showError(
                FireBirdUIMessages.dialog_events_title,
                FireBirdUIMessages.dialog_events_error_no_events
            );
            return;
        }

        String[] parts = rawEvents.split(",");
        List<String> eventNames = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                eventNames.add(trimmed);
            }
        }
        if (eventNames.isEmpty()) {
            DBWorkbench.getPlatformUI().showError(
                FireBirdUIMessages.dialog_events_title,
                FireBirdUIMessages.dialog_events_error_no_events
            );
            return;
        }

        try {
            initEventManager(eventNames);
            activeEvents.clear();
            activeEvents.addAll(eventNames);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            eventsText.setEnabled(false);

            logMessage(NLS.bind(FireBirdUIMessages.dialog_events_log_started, String.join(", ", eventNames)));
        } catch (Exception e) {
            log.error("Error starting Firebird event listener", e);
            DBWorkbench.getPlatformUI().showError(
                FireBirdUIMessages.dialog_events_error_connect,
                e.getMessage(),
                e
            );
            stopListening();
        }
    }

    private void stopListening() {
        if (eventManager != null) {
            try {
                if (listenerProxy != null && eventListenerClass != null) {
                    Method removeListenerMethod = eventManager.getClass().getMethod("removeEventListener", String.class, eventListenerClass);
                    for (String eventName : activeEvents) {
                        try {
                            removeListenerMethod.invoke(eventManager, eventName, listenerProxy);
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            try {
                Method disconnectMethod = eventManager.getClass().getMethod("disconnect");
                disconnectMethod.invoke(eventManager);
            } catch (Exception e) {
                log.warn("Error disconnecting FBEventManager", e);
            } finally {
                eventManager = null;
                listenerProxy = null;
                eventListenerClass = null;
                activeEvents.clear();
            }
        }

        if (startButton != null && !startButton.isDisposed()) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            eventsText.setEnabled(true);
            logMessage(FireBirdUIMessages.dialog_events_log_stopped);
        }
    }

    private void initEventManager(List<String> eventNames) throws DBException {
        DBPDataSourceContainer container = dataSource.getContainer();
        DBPDriver driver = container.getDriver();
        DBPDriverLoader loader = driver.getDriverLoader(container);
        ClassLoader classLoader = loader.getClassLoader();
        if (classLoader == null) {
            try {
                UIUtils.runInProgressService(loader::loadDriver);
            } catch (Exception e) {
                log.error("Cannot load driver", e);
            }
            classLoader = loader.getClassLoader();
        }
        if (classLoader == null) {
            classLoader = FireBirdDataSource.class.getClassLoader();
        }

        Class<?> eventManagerClass = null;
        String[] managerNames = new String[] {
            "org.firebirdsql.event.FBEventManager",
            "org.firebirdsql.management.FBEventManager"
        };
        for (String name : managerNames) {
            try {
                eventManagerClass = Class.forName(name, true, classLoader);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (eventManagerClass == null) {
            throw new DBException(FireBirdUIMessages.dialog_events_error_driver);
        }

        String[] listenerNames = new String[] {
            "org.firebirdsql.event.EventListener",
            "org.firebirdsql.management.EventListener"
        };
        for (String name : listenerNames) {
            try {
                eventListenerClass = Class.forName(name, true, classLoader);
                break;
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (eventListenerClass == null) {
            throw new DBException(FireBirdUIMessages.dialog_events_error_driver);
        }

        try {
            eventManager = eventManagerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new DBException("Cannot instantiate " + eventManagerClass.getName(), e);
        }

        DBPConnectionConfiguration cfg = container.getActualConnectionConfiguration();
        String host = cfg.getHostName();
        if (CommonUtils.isEmpty(host)) {
            host = cfg.getServerName();
        }
        if (CommonUtils.isEmpty(host)) {
            host = "localhost";
        }
        int port = CommonUtils.toInt(cfg.getHostPort(), 3050);
        String database = cfg.getDatabaseName();
        String user = cfg.getUserName();
        String password = cfg.getUserPassword();

        setProperty(eventManager, new String[]{"setServerName", "setHost"}, String.class, host);
        if (port > 0) {
            setProperty(eventManager, new String[]{"setPortNumber", "setPort"}, int.class, port);
        }
        if (!CommonUtils.isEmpty(database)) {
            setProperty(eventManager, new String[]{"setDatabaseName", "setDatabase"}, String.class, database);
        }
        if (!CommonUtils.isEmpty(user)) {
            setProperty(eventManager, new String[]{"setUser", "setUserName"}, String.class, user);
        }
        if (!CommonUtils.isEmpty(password)) {
            setProperty(eventManager, new String[]{"setPassword"}, String.class, password);
        }

        // Connect
        try {
            Method connectMethod = eventManager.getClass().getMethod("connect");
            connectMethod.invoke(eventManager);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new DBException("Failed to connect to Firebird event manager: " + cause.getMessage(), cause);
        }

        // Create listener proxy
        listenerProxy = Proxy.newProxyInstance(
            classLoader,
            new Class<?>[]{eventListenerClass},
            (proxy, method, args) -> {
                if ("eventOccurred".equals(method.getName()) && args != null && args.length > 0) {
                    Object dbEvent = args[0];
                    String eventName = "";
                    int count = 1;
                    try {
                        Method getEventName = dbEvent.getClass().getMethod("getEventName");
                        eventName = String.valueOf(getEventName.invoke(dbEvent));
                    } catch (Exception ignored) {
                    }
                    try {
                        Method getEventCount = dbEvent.getClass().getMethod("getEventCount");
                        Object cnt = getEventCount.invoke(dbEvent);
                        if (cnt instanceof Number) {
                            count = ((Number) cnt).intValue();
                        }
                    } catch (Exception ignored) {
                    }
                    onEventReceived(eventName, count);
                    return null;
                }
                if ("toString".equals(method.getName())) {
                    return "FireBirdEventListenerProxy";
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(method.getName())) {
                    return args.length > 0 && args[0] == proxy;
                }
                return null;
            }
        );

        // Register event listeners
        try {
            Method addListenerMethod = eventManager.getClass().getMethod("addEventListener", String.class, eventListenerClass);
            for (String eventName : eventNames) {
                addListenerMethod.invoke(eventManager, eventName, listenerProxy);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new DBException("Failed to add event listener: " + cause.getMessage(), cause);
        }
    }

    private void onEventReceived(@NotNull String name, int count) {
        logMessage(NLS.bind(FireBirdUIMessages.dialog_events_log_event_received, name, count));
    }

    private void logMessage(@NotNull String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = String.format("[%s] %s%n", timestamp, message);
        UIUtils.asyncExec(() -> {
            if (logText != null && !logText.isDisposed()) {
                logText.append(line);
            }
        });
    }

    private static void setProperty(@NotNull Object target, @NotNull String[] methodNames, @NotNull Class<?> paramType, @Nullable Object value) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name, paramType);
                m.invoke(target, value);
                return;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.debug("Error invoking " + name + " on " + target.getClass().getName(), e);
            }
        }
    }

    @Override
    public boolean close() {
        stopListening();
        return super.close();
    }
}
