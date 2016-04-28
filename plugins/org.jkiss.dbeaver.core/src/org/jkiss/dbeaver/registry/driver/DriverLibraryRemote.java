/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
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
