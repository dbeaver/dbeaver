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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.WebUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;

/**
 * DriverLibraryAbstract
 */
public abstract class DriverLibraryAbstract implements DBPDriverLibrary
{
    static final Log log = Log.getLog(DriverLibraryAbstract.class);

    protected final DriverDescriptor driver;
    protected final FileType type;
    protected final OSDescriptor system;
    protected final String path;
    protected boolean custom;
    protected boolean disabled;

    public static DriverLibraryAbstract createFromPath(DriverDescriptor driver, FileType type, String path) {
        if (path.startsWith(DriverLibraryRepository.PATH_PREFIX)) {
            return new DriverLibraryRepository(driver, type, path);
        } else if (path.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
            return new DriverLibraryMavenArtifact(driver, type, path);
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
        this.type = FileType.valueOf(config.getAttribute(RegistryConstants.ATTR_TYPE));

        String osName = config.getAttribute(RegistryConstants.ATTR_OS);
        this.system = osName == null ? null : new OSDescriptor(
            osName,
            config.getAttribute(RegistryConstants.ATTR_ARCH));
        this.path = config.getAttribute(RegistryConstants.ATTR_PATH);
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

    @NotNull
    @Override
    public DBPDriverLibrary createVersion(DBRProgressMonitor monitor, @NotNull String version) throws IOException {
        throw new IOException("Versions are not supported");
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

        final URLConnection connection = WebUtils.openConnection(externalURL);

        int contentLength = connection.getContentLength();
        if (contentLength < 0) {
            contentLength = 0;
        }
        int bufferLength = contentLength / 10;
        if (bufferLength > 1000000) {
            bufferLength = 1000000;
        }
        if (bufferLength < 50000) {
            bufferLength = 50000;
        }
        monitor.beginTask(taskName + " - " + externalURL, contentLength);
        boolean success = false;
        try (final OutputStream outputStream = new FileOutputStream(localFile)) {
            try (final InputStream inputStream = connection.getInputStream()) {
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                byte[] buffer = new byte[bufferLength];
                int totalRead = 0;
                for (;;) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    //monitor.subTask(numberFormat.format(totalRead) + "/" + numberFormat.format(contentLength));
                    final int count = inputStream.read(buffer);
                    if (count <= 0) {
                        success = true;
                        break;
                    }
                    outputStream.write(buffer, 0, count);
                    monitor.worked(count);
                    totalRead += count;
                }
            }
        } finally {
            if (!success) {
                if (!localFile.delete()) {
                    log.warn("Can't delete local driver file '" + localFile.getAbsolutePath() + "'");
                }
            }
            monitor.done();
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

}
