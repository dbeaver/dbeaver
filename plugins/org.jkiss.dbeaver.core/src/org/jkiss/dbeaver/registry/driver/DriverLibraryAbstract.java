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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.net.URLConnection;
import java.text.NumberFormat;

/**
 * DriverLibraryAbstract
 */
public abstract class DriverLibraryAbstract implements DBPDriverLibrary
{
    static final Log log = Log.getLog(DriverLibraryAbstract.class);

    protected final DriverDescriptor driver;
    protected final FileType type;
    protected final OSDescriptor system;
    protected String path;
    protected boolean custom;
    protected boolean disabled;

    public static DriverLibraryAbstract createFromPath(DriverDescriptor driver, FileType type, String path) {
        if (path.startsWith(DriverLibraryRepository.PATH_PREFIX)) {
            return new DriverLibraryRepository(driver, type, path);
        } else if (path.startsWith(DriverLibraryMavenArtifact.PATH_PREFIX)) {
            return new DriverLibraryMavenArtifact(driver, type, path);
        } else {
            return new DriverLibraryLocal(driver, type, path);
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
            return new DriverLibraryLocal(driver, config);
        }
    }

    protected DriverLibraryAbstract(DriverDescriptor driver, FileType type, String path)
    {
        this.driver = driver;
        this.type = type;
        this.system = DBeaverCore.getInstance().getLocalSystem();
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

    public void downloadLibraryFile(DBRProgressMonitor monitor, boolean forceUpdate) throws IOException, InterruptedException
    {
        String externalURL = getExternalURL();
        if (externalURL == null) {
            throw new IOException("Unresolved file reference: " + getPath());
        }

        final URLConnection connection = RuntimeUtils.openConnection(externalURL);
        monitor.worked(1);
        monitor.done();

        final int contentLength = connection.getContentLength();
        monitor.beginTask("Download " + externalURL, contentLength);
        boolean success = false;
        final File localFile = getLocalFile();
        if (localFile == null) {
            throw new IOException("No target file for '" + getPath() + "'");
        }
        final File localDir = localFile.getParentFile();
        if (!localDir.exists()) {
            if (!localDir.mkdirs()) {
                log.warn("Can't create directory for local driver file '" + localDir.getAbsolutePath() + "'");
            }
        }
        final OutputStream outputStream = new FileOutputStream(localFile);
        try {
            final InputStream inputStream = connection.getInputStream();
            try {
                final NumberFormat numberFormat = NumberFormat.getNumberInstance();
                byte[] buffer = new byte[10000];
                int totalRead = 0;
                for (;;) {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    monitor.subTask(numberFormat.format(totalRead) + "/" + numberFormat.format(contentLength));
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
            finally {
                ContentUtils.close(inputStream);
            }
        } finally {
            ContentUtils.close(outputStream);
            if (!success) {
                if (!localFile.delete()) {
                    log.warn("Can't delete local driver file '" + localFile.getAbsolutePath() + "'");
                }
            }
        }
        monitor.done();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

}
