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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryRepository extends DriverLibraryLocal
{
    public static final String PATH_PREFIX = "repo:/";

    public DriverLibraryRepository(DriverDescriptor driver, FileType type, String path) {
        super(driver, type, path);
    }

    public DriverLibraryRepository(DriverDescriptor driver, IConfigurationElement config) {
        super(driver, config);
    }

    @Override
    public boolean isDownloadable()
    {
        return true;
    }

    @Override
    protected String getLocalFilePath() {
        return path.substring(PATH_PREFIX.length());
    }

    @Nullable
    @Override
    public String getExternalURL(DBRProgressMonitor monitor) {
        String localPath = getLocalFilePath();
        String primarySource = DriverDescriptor.getDriversPrimarySource();
        if (!primarySource.endsWith("/") && !localPath.startsWith("/")) {
            primarySource += '/';
        }
        return primarySource + localPath;
    }



}
