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
package org.jkiss.dbeaver.registry.driver;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.maven.*;
import org.jkiss.dbeaver.ui.UIIcon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DriverLibraryDescriptor
 */
public class DriverLibraryMavenDependency extends DriverLibraryMavenArtifact
{
    private MavenLocalVersion localVersion;

    public DriverLibraryMavenDependency(DriverDescriptor driverDescriptor, MavenLocalVersion localVersion) {
        super(driverDescriptor, FileType.jar, localVersion.getArtifact().toString());
        this.localVersion = localVersion;
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    @Nullable
    private MavenArtifact getMavenArtifact() {
        return localVersion.getArtifact();
    }

    @Nullable
    @Override
    public String getExternalURL() {
        return localVersion.getExternalURL(MavenArtifact.FILE_JAR);
    }


    @Nullable
    @Override
    public File getLocalFile()
    {
        return localVersion.getCacheFile();
    }

    @NotNull
    public String getDisplayName() {
        return localVersion.getArtifact().getGroupId() + ":" + localVersion.getArtifact().getArtifactId() + ":" + localVersion.getVersion();
    }

    protected MavenLocalVersion resolveLocalVersion(DBRProgressMonitor monitor, boolean forceUpdate) throws IOException {
        return localVersion;
    }

}
