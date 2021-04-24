/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.osgi.internal.framework.BundleContextImpl;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.notifications.NotificationUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.hooks.bundle.EventHook;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoreApplicationActivator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.ui.app.standalone";

    private static final boolean PATCH_ECLIPSE_CLASSES = false;

    // The shared instance
    private static CoreApplicationActivator plugin;

    public CoreApplicationActivator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        if (PATCH_ECLIPSE_CLASSES) {
            activateHooks(context);
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

        // Add bundle load logger
        if (!Log.isQuietMode()) {
            context.registerService(EventHook.class, (event, contexts) -> {
                String message = null;
                Bundle bundle = event.getBundle();
                if (event.getType() == BundleEvent.STARTED) {
                    if (bundle.getState() == Bundle.ACTIVE) {
                        message = "> Start " + getBundleName(bundle) + " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                    }
                } else if (event.getType() == BundleEvent.STOPPING) {
                    if (bundle.getState() != BundleEvent.STOPPING && bundle.getState() != BundleEvent.UNINSTALLED) {
                        message = "< Stop " + getBundleName(bundle) + " [" + bundle.getSymbolicName() + " " + bundle.getVersion() + "]";
                    }
                }
                if (message != null) {
                    System.err.println(message);
                }
            }, null);
            //context.addBundleListener(new BundleLoadListener());
        }

        plugin = this;
    }

    private static String getBundleName(Bundle bundle) {
        String bundleName = bundle.getHeaders().get("Bundle-Name");
        if (CommonUtils.isEmpty(bundleName)) {
            bundleName = bundle.getSymbolicName();
        }
        return bundleName;
    }

    private void activateHooks(BundleContext context) {
        EquinoxContainer container = ((BundleContextImpl)context).getContainer();
        HookRegistry registry = container.getConfiguration().getHookRegistry();
        List<ClassLoaderHook> hooks = new ArrayList<>(registry.getClassLoaderHooks());
        hooks.add(new PatchClassLoaderHook());
        List<ClassLoaderHook> newHooks = Collections.unmodifiableList(hooks);

        try {
            Field hooksField = registry.getClass().getDeclaredField("classLoaderHooksRO");
            hooksField.setAccessible(true);
            hooksField.set(registry, newHooks);
        } catch (Throwable e) {
            getLog().error("Error initializing class loader hook", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static CoreApplicationActivator getDefault() {
        return plugin;
    }

}
