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
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract application implementation
 */
public abstract class AbstractApplication implements IApplication, DBPApplication {

    private static final Log log = Log.getLog(AbstractApplication.class);

    public static final Integer EXIT_ERROR_UNSPECIFIED = 1;

    private static DBPApplication INSTANCE;

    private static final class ConsoleUIHolder {
        private static final DBPPlatformUI INSTANCE = new ConsoleUserInterface();
    }

    private String applicationRunId;
    private final long applicationStartTime = System.currentTimeMillis();
    private DBPPlatform platform;
    private DBPPlatformUI platformUI;

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

    @NotNull
    @Override
    public Map<String, String> getAdditionalApplicationProperties() {
        return Map.of();
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
    public String getInfoDetails() {
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

    @NotNull
    @Override
    public Object start(IApplicationContext context) throws Exception {
        return EXIT_OK;
    }

    @Override
    public void stop() {

    }

    protected void initializeApplicationServices() {
        platform = createService(getPlatformClass());
        Class<? extends DBPPlatformUI> platformUIClass = getPlatformUIClass();
        platformUI = platformUIClass == null ? null : createService(platformUIClass);
    }

    @NotNull
    public DBPPlatform getPlatformInstance() {
        if (platform == null) {
            throw new IllegalStateException("Application platform is not instantiated");
        }
        return platform;
    }

    @NotNull
    public DBPPlatformUI getPlatformUIInstance() {
        return platformUI == null ? ConsoleUIHolder.INSTANCE : platformUI;
    }

    public void initializePlatform() {
        initializeService(getPlatformInstance());
        if (platformUI != null) {
            initializeService(platformUI);
        }
    }

    @Nullable
    private <T> T createService(@NotNull Class<? extends T> serviceImplClass) {
        try {
            Constructor<? extends T> constructor = serviceImplClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable e) {
            log.error("Error instantiating service '" + serviceImplClass.getName() + "'", e);
            return null;
        }
    }

    private static void initializeService(@NotNull Object service) {
        try {
            Method method = service.getClass().getDeclaredMethod("initialize");
            method.setAccessible(true);
            method.invoke(service);
        } catch (Exception e) {
            if (e instanceof InvocationTargetException ite && ite.getTargetException() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e);
        }
    }

}
