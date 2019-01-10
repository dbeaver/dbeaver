/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * DriverLibraryRemote
 */
public class DriverLibraryRemote extends DriverLibraryLocal
{
    private static final Log log = Log.getLog(DriverLibraryRemote.class);

    public static final String DOWNLOAD_DIR = "remote";

    public static final String[] SUPPORTED_PROTOCOLS = {
        "http",
        "https",
        "ftp",
    };

    public DriverLibraryRemote(DriverDescriptor driver, FileType type, String url) {
        super(driver, type, url);
    }

    public DriverLibraryRemote(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
    }

    private DriverLibraryRemote(DriverDescriptor driver, DriverLibraryRemote copyFrom) {
        super(driver, copyFrom);
    }

    @Override
    public DBPDriverLibrary copyLibrary(DriverDescriptor driverDescriptor) {
        return new DriverLibraryRemote(driverDescriptor, this);
    }

    @Override
    public boolean isDownloadable()
    {
        return true;
    }

    @Override
    protected String getLocalFilePath() {
        try {
            final String path = new URL(getPath()).getPath();
            if (path.startsWith("/")) {
                return DOWNLOAD_DIR + path;
            } else {
                return DOWNLOAD_DIR + "/" + path;
            }
        } catch (MalformedURLException e) {
            log.error(e);
            return getPath();
        }
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        return getPath();
    }


    public static boolean supportsURL(String url) {
        int pos = url.indexOf(":/");
        if (pos <= 0) {
            return false;
        }
        return ArrayUtils.contains(SUPPORTED_PROTOCOLS, url.substring(0, pos));
    }
}
