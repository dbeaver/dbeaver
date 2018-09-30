/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.util.Collection;
import java.util.Collections;

/**
 * DriverLibraryAbstract
 */
public abstract class DriverLibraryAbstract implements DBPDriverLibrary
{
    private static final Log log = Log.getLog(DriverLibraryAbstract.class);

    protected final DriverDescriptor driver;
    protected final FileType type;
    protected final OSDescriptor system;
    protected String path;
    private boolean optional;
    protected boolean custom;
    protected boolean disabled;

    public static DriverLibraryAbstract createFromPath(DriverDescriptor driver, FileType type, String path, String preferredVersion) {
        if (path.startsWith(DriverLibraryRepository.PATH_PREFIX)) {
            return new DriverLibraryRepository(driver, type, path);
        } else if (path.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
            return new DriverLibraryMavenArtifact(driver, type, path, preferredVersion);
        } else {
            if (DriverLibraryRemote.supportsURL(path)) {
                return new DriverLibraryRemote(driver, type, path);
            } else {
                return new DriverLibraryLocal(driver, type, path);
            }
        }
    }

    public static DriverLibraryAbstract createFromConfig(DriverDescriptor driver, IConfigurationElement config) {
        String path = config.getAttribute(RegistryConstants.ATTR_PATH);
        if (CommonUtils.isEmpty(path)) {
            log.error("Bad file path");
            return null;
        }

        if (!DriverUtils.matchesBundle(config)) {
            return null;
        }


        if (path.startsWith(DriverLibraryRepository.PATH_PREFIX)) {
            return new DriverLibraryRepository(driver, config);
        } else if (path.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
            return new DriverLibraryMavenArtifact(driver, config);
        } else {
            if (DriverLibraryRemote.supportsURL(path)) {
                return new DriverLibraryRemote(driver, config);
            } else {
                return new DriverLibraryLocal(driver, config);
            }
        }
    }

    protected DriverLibraryAbstract(DriverDescriptor driverDescriptor, DriverLibraryAbstract copyFrom) {
        this.driver = driverDescriptor;
        this.type = copyFrom.type;
        this.system = copyFrom.system;
        this.path = copyFrom.path;
        this.optional = copyFrom.optional;
        this.custom = copyFrom.custom;
        this.disabled = copyFrom.disabled;
    }

    protected DriverLibraryAbstract(DriverDescriptor driver, FileType type, String path)
    {
        this.driver = driver;
        this.type = type;
        this.system = null;
        this.path = path;
        this.custom = true;
    }

    protected DriverLibraryAbstract(DriverDescriptor driver, IConfigurationElement config)
    {
        this.driver = driver;
        String typeStr = config.getAttribute(RegistryConstants.ATTR_TYPE);
        if ("zip".equalsIgnoreCase(typeStr)) {
            typeStr = FileType.jar.name();
        }
        this.type = FileType.valueOf(typeStr);

        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.system = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));
        this.path = config.getAttribute(RegistryConstants.ATTR_PATH);
        this.optional = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_OPTIONAL), false);
        this.custom = false;
    }

    public DriverDescriptor getDriver()
    {
        return driver;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @NotNull
    @Override
    public Collection<String> getAvailableVersions(DBRProgressMonitor monitor) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public String getPreferredVersion() {
        return null;
    }

    @Override
    public void setPreferredVersion(String version) {
        // do nothing
    }

    @NotNull
    @Override
    public FileType getType()
    {
        return type;
    }

    @NotNull
    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public boolean isCustom()
    {
        return custom;
    }

    public void setCustom(boolean custom)
    {
        this.custom = custom;
    }

    @Override
    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    @Override
    public boolean matchesCurrentPlatform()
    {
        return system == null || system.matches(DBeaverCore.getInstance().getLocalSystem());
    }

    public void downloadLibraryFile(@NotNull DBRProgressMonitor monitor, boolean forceUpdate, String taskName) throws IOException, InterruptedException
    {
        final File localFile = getLocalFile();
        if (localFile == null) {
            throw new IOException("No target file for '" + getPath() + "'");
        }
        if (!forceUpdate && localFile.exists()) {
            return;
        }
        final File localDir = localFile.getParentFile();
        if (!localDir.exists()) {
            if (!localDir.mkdirs()) {
                log.warn("Can't create directory for local driver file '" + localDir.getAbsolutePath() + "'");
            }
        }

        String externalURL = getExternalURL(monitor);
        if (externalURL == null) {
            throw new IOException("Unresolved file reference: " + getPath());
        }

        WebUtils.downloadRemoteFile(monitor, taskName, externalURL, localFile, getAuthInfo(monitor));
    }

    @Nullable
    protected DBAAuthInfo getAuthInfo(DBRProgressMonitor monitor) {
        return null;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public abstract DBPDriverLibrary copyLibrary(DriverDescriptor driverDescriptor);
}
