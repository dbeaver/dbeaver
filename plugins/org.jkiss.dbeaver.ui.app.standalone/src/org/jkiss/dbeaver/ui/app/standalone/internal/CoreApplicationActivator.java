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

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.notifications.NotificationUtils;
import org.jkiss.dbeaver.utils.OsgiUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CoreApplicationActivator extends AbstractUIPlugin {

    // The shared instance
    private static CoreApplicationActivator plugin;

    public CoreApplicationActivator() {
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);

        trackBundleActivation(context);
        configureNotifications();

        plugin = this;
    }

    private static void configureNotifications() {
        // Set notifications handler
        DBeaverNotifications.setHandler(new DBeaverNotifications.NotificationHandler() {
            @Override
            public void sendNotification(
                @NotNull DBPDataSource dataSource,
                @NotNull String id,
                @NotNull String text,
                @Nullable DBPMessageType messageType,
                @Nullable Runnable feedback
            ) {
                NotificationUtils.sendNotification(dataSource, id, text, messageType, feedback);
            }

            @Override
            public void sendNotification(
                @NotNull String id,
                @NotNull String title,
                @NotNull String text,
                @Nullable DBPMessageType messageType,
                @Nullable Runnable feedback
            ) {
                NotificationUtils.sendNotification(id, title, text, messageType, feedback);
            }
        });
    }

    private static void trackBundleActivation(@NotNull BundleContext context) {
        // Add bundle load logger
        if (!Log.isQuietMode()) {
            Set<String> activatedBundles = new HashSet<>();
            context.registerService(EventHook.class, (event, contexts) -> {
                String message = null;
                Bundle bundle = event.getBundle();
                if (event.getType() == BundleEvent.STARTED) {
                    if (bundle.getState() == Bundle.ACTIVE) {
                        message = "> Start " + OsgiUtils.getBundleName(bundle) +
                            " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                        activatedBundles.add(bundle.getSymbolicName());
                    }
                } else if (event.getType() == BundleEvent.STOPPING) {
                    activatedBundles.remove(bundle.getSymbolicName());
                    //message = "< Stop " + getBundleName(bundle) +
                    // " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                }
                if (message != null) {
                    System.err.println(message);
                }
            }, null);
        }
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    @NotNull
    public static CoreApplicationActivator getDefault() {
        return Objects.requireNonNull(plugin, "Core UI plugin was not started");
    }

}
