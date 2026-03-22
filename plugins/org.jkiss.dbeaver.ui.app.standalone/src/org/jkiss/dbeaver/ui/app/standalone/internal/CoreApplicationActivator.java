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
package org.jkiss.dbeaver.ui.app.standalone.internal;

import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.notifications.NotificationUtils;
import org.jkiss.dbeaver.utils.OsgiUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.bundle.EventHook;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class CoreApplicationActivator extends AbstractUIPlugin {

    private static final Log log = Log.getLog(CoreApplicationActivator.class);

    // The shared instance
    private static CoreApplicationActivator plugin;

    private static ServiceRegistration<IProxyService> proxyService;

    public CoreApplicationActivator() {
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);

        // Add bundle load logger
        if (!Log.isQuietMode()) {
            Set<String> activatedBundles = new HashSet<>();
            context.registerService(EventHook.class, (event, contexts) -> {
                String message = null;
                Bundle bundle = event.getBundle();
                if (event.getType() == BundleEvent.STARTED) {
                    if (bundle.getState() == Bundle.ACTIVE) {
                        message = "> Start " + OsgiUtils.getBundleName(bundle) + " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                        activatedBundles.add(bundle.getSymbolicName());
                    }
                } else if (event.getType() == BundleEvent.STOPPING) {
                    if (activatedBundles.remove(bundle.getSymbolicName())) {
                        //message = "< Stop " + getBundleName(bundle) + " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                    }
                }
                if (message != null) {
                    System.err.println(message);
                }
            }, null);
        }

        // Set notifications handler
        DBeaverNotifications.setHandler(new DBeaverNotifications.NotificationHandler() {
            @Override
            public void sendNotification(DBPDataSource dataSource, String id, String text, DBPMessageType messageType, Runnable feedback) {
                NotificationUtils.sendNotification(dataSource, id, text, messageType, feedback);
            }

            @Override
            public void sendNotification(String id, String title, String text, DBPMessageType messageType, Runnable feedback) {
                NotificationUtils.sendNotification(id, title, text, messageType, feedback);
            }
        });

        // Configure proxy
        activateProxyService(context);

        plugin = this;
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        if (proxyService != null) {
            proxyService.unregister();
            proxyService = null;
        }

        plugin = null;
        super.stop(context);
    }

    @NotNull
    public static CoreApplicationActivator getDefault() {
        return plugin;
    }

    private static void activateProxyService(@NotNull BundleContext context) {
        // Activate Eclipse proxy service UI queue
        // It may require master password and already initialized platform
        UIExecutionQueue.queueExec(() -> {
            try {
                ProxyManager proxyManager = (ProxyManager) ProxyManager
                    .getProxyManager();
                proxyManager.initialize();
                proxyService = context.registerService(IProxyService.class, proxyManager, new Hashtable<>());
            } catch (Throwable e) {
                log.debug("Proxy service activation has failed", e);
            }
        });
    }


}
