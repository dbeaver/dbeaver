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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.nio.file.Path;

/**
 * DriverLibraryRemote
 */
public class DriverLibraryRemote extends DriverLibraryLocal {
    private static final Log log = Log.getLog(DriverLibraryRemote.class);

    public static final String DOWNLOAD_DIR = "remote";

    public static final String[] SUPPORTED_PROTOCOLS = {
        "http",
        "https",
        "ftp",
    };
    private String customLocalFileName;

    public DriverLibraryRemote(@NotNull  DriverDescriptor driver, @NotNull FileType type, @NotNull String url) {
        super(driver, type, url);
    }

    public DriverLibraryRemote(@Nullable DriverDescriptor driver, @NotNull IConfigurationElement config) {
        super(driver, config);
    }

    private DriverLibraryRemote(@NotNull DriverDescriptor driver, @NotNull DriverLibraryRemote copyFrom) {
        super(driver, copyFrom);
    }

    @NotNull
    @Override
    public DBPDriverLibrary copyLibrary(@NotNull DriverDescriptor driverDescriptor) {
        return new DriverLibraryRemote(driverDescriptor, this);
    }

    @Override
    public boolean isDownloadable()
    {
        return true;
    }

    @Override
    protected String getLocalFilePath() {
        String finalPath = URI.create(getPath()).getPath();
        String customLocalFileName = getCustomLocalFileName();
        if (!CommonUtils.isEmpty(customLocalFileName)) {
            // Replace local file name with custom
            // Sometimes remote URL is dummy (e.g. driver.zip or some random UUID) and here we can rewrite it
            Path folder = Path.of(finalPath).getParent();
            if (folder != null) {
                finalPath = folder.resolve(customLocalFileName).toString();
            }
        }

        if (finalPath.startsWith("/")) {
            return DOWNLOAD_DIR + finalPath;
        } else {
            return DOWNLOAD_DIR + "/" + finalPath;
        }
    }

    @Nullable
    @Override
    public String getExternalURL(@NotNull DBRProgressMonitor monitor) {
        return getPath();
    }

    @Nullable
    public String getCustomLocalFileName() {
        return customLocalFileName;
    }

    public void setCustomLocalFileName(@Nullable String customLocalFileName) {
        this.customLocalFileName = customLocalFileName;
    }

    public static boolean supportsURL(String url) {
        int pos = url.indexOf(":/");
        if (pos <= 0) {
            return false;
        }
        return ArrayUtils.contains(SUPPORTED_PROTOCOLS, url.substring(0, pos));
    }
}
