/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * DriverLibraryBundle
 */
public class DriverLibraryBundle extends DriverLibraryAbstract {

    public static final String PATH_PREFIX = "bundle:/";

    private static final Log log = Log.getLog(DriverLibraryBundle.class);

    private Bundle bundle;

    public DriverLibraryBundle(DriverDescriptor driver, FileType type, String path) {
        super(driver, type, path);
    }

    public DriverLibraryBundle(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
    }

    public DriverLibraryBundle(DriverDescriptor driverDescriptor, DriverLibraryBundle copyFrom) {
        super(driverDescriptor, copyFrom);
    }

    @NotNull
    @Override
    public DBPDriverLibrary copyLibrary(@NotNull DriverDescriptor driverDescriptor) {
        return new DriverLibraryBundle(driverDescriptor, this);
    }

    @Override
    public boolean isDownloadable() {
        return false;
    }

    @Override
    public void resetVersion() {
        // do nothing
    }

    @Override
    public boolean isSecureDownload(@NotNull DBRProgressMonitor monitor) {
        return true;
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        return null;
    }

    @Nullable
    @Override
    public Path getLocalFile() {
        if (bundle == null) {
            bundle = findBundle();
        }
        if (bundle == null) {
            return null;
        }
        try {
            String location = bundle.getLocation();
            int divPos = location.indexOf("file:");
            if (divPos != -1) {
                String installPath = location.substring(divPos + 5);
                return RuntimeUtils.getLocalPathFromURL(Platform.getInstallLocation().getURL()).resolve(installPath);
            }
        } catch (Exception e) {
            log.debug(e);
        }
        return null;
    }

    private Bundle findBundle() {
        Bundle curBundle = FrameworkUtil.getBundle(getClass());
        if (curBundle == null) {
            return null;
        }
        String bundleId = getPath().substring(PATH_PREFIX.length());
        BundleContext context = curBundle.getBundleContext();
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(bundleId)) {
                return bundle;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException {
        return null;
    }

    @NotNull
    public String getDisplayName() {
        return path;
    }

    @NotNull
    @Override
    public String getId() {
        return path;
    }

    @NotNull
    @Override
    public DBIcon getIcon() {
        return DBIcon.JAR;
    }

}
