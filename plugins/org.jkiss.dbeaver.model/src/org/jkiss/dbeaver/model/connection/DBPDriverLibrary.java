/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Driver library
 */
public interface DBPDriverLibrary
{

    /**
     * Driver file type
     */
    enum FileType
    {
        jar,
        lib,
        executable,
        license
    }

    @NotNull
    String getDisplayName();

    /**
     * Library native id.
     * Id doesn't include version information so the same libraries with different versions have the same id.
     */
    String getId();

    /**
     * Library version. If library doesn't support versions returns null.
     */
    @Nullable
    String getVersion();

    @NotNull
    DBIcon getIcon();

    @NotNull
    FileType getType();

    /**
     * Native library URI.
     * Could be a file path or maven artifact references or anything else.
     */
    @NotNull
    String getPath();

    @Nullable
    String getDescription();

    boolean isCustom();

    boolean isDisabled();

    void setDisabled(boolean disabled);

    boolean isDownloadable();

    @Nullable
    String getExternalURL(DBRProgressMonitor monitor);

    @Nullable
    File getLocalFile();

    boolean matchesCurrentPlatform();

    @Nullable
    Collection<? extends DBPDriverLibrary> getDependencies(@NotNull DBRProgressMonitor monitor) throws IOException;

    void downloadLibraryFile(@NotNull DBRProgressMonitor monitor, boolean forceUpdate, String taskName)
        throws IOException, InterruptedException;

    @NotNull
    Collection<String> getAvailableVersions(DBRProgressMonitor monitor) throws IOException;

    @NotNull
    DBPDriverLibrary createVersion(DBRProgressMonitor monitor, @NotNull String version)
        throws IOException;

    void resetVersion();

}
