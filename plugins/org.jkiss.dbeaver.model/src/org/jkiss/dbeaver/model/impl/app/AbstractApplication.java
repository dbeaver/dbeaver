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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.Constructor;
import java.util.UUID;

/**
 * Abstract application implementation
 */
public abstract class AbstractApplication implements IApplication, DBPApplication {

    private static final Log log = Log.getLog(AbstractApplication.class);

    private static DBPApplication INSTANCE;

    private String applicationRunId;
    private final long applicationStartTime = System.currentTimeMillis();

    protected AbstractApplication() {
        if (INSTANCE != null) {
            log.error("Multiple application instances created: " + INSTANCE.getClass().getName() + ", " + this.getClass().getName());
        }
        INSTANCE = this;
    }

    public static DBPApplication getInstance() {
        if (INSTANCE == null) {
            DBPApplication instance = null;
            try {
                instance = ApplicationRegistry.getInstance().getApplication().getInstance();
            } catch (Throwable e) {
                log.error(e);
            }
            if (instance == null) {
                throw new IllegalStateException("No DBeaver application found");
            }
            INSTANCE = instance;
        }
        return INSTANCE;
    }

    public boolean isStandalone() {
        return true;
    }

    @Override
    public boolean isPrimaryInstance() {
        return true;
    }

    @Override
    public boolean isHeadlessMode() {
        return false;
    }

    @Override
    public boolean isExclusiveMode() {
        return false;
    }

    @Override
    public boolean isMultiuser() {
        return false;
    }

    @Override
    public boolean isDistributed() {
        return false;
    }

    @Override
    public boolean isDetachedProcess() {
        return false;
    }

    @NotNull
    public String getApplicationRunId() {
        if (applicationRunId == null) {
            applicationRunId = UUID.randomUUID().toString();
        }
        return applicationRunId;
    }

    @Override
    public long getApplicationStartTime() {
        return applicationStartTime;
    }

    @Override
    public String getInfoDetails(DBRProgressMonitor monitor) {
        return "N/A";
    }

    @Nullable
    @Override
    public String getProductProperty(@NotNull String propName) {
        return Platform.getProduct().getProperty(propName);
    }

    @Override
    public boolean hasProductFeature(@NotNull String featureName) {
        // By default, product includes almost all possible features
        // Feature set can be customized by particular implementation
        return switch (featureName) {
            case DBFUtils.PRODUCT_FEATURE_MULTI_FS -> false;
            default -> true;
        };
    }

    /////////////////////////////////////////
    // IApplication

    @Override
    public Object start(IApplicationContext context) throws Exception {
        return EXIT_OK;
    }

    @Override
    public void stop() {

    }

    protected void initializeApplicationServices() {
        if (getClass().getClassLoader() instanceof BundleReference br) {
            // Initialize platform
            BundleContext bundleContext = br.getBundle().getBundleContext();
            if (bundleContext == null) {
                // Use model bundle context
                bundleContext = FrameworkUtil.getBundle(DBPApplication.class).getBundleContext();
            }
            registerService(bundleContext, DBPPlatform.class, getPlatformClass());
            registerService(bundleContext, DBPPlatformUI.class, getPlatformUIClass());
        } else {
            log.error("Cannot initialize application services in non-OSGI context");
        }
    }

    protected <T> void registerService(BundleContext bundleContext, Class<T> serviceInt, Class<? extends T> serviceImplClass) {
        if (serviceImplClass == null) {
            return;
        }
        try {
            Constructor<? extends T> constructor = serviceImplClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T serviceImpl = constructor.newInstance();
            bundleContext.registerService(serviceInt, serviceImpl, null);
        } catch (Throwable e) {
            log.error("Error instantiating service '" + serviceInt.getName() + "'", e);
        }
    }

}
