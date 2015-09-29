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
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.maven.MavenArtifact;
import org.jkiss.dbeaver.registry.maven.MavenArtifactReference;
import org.jkiss.dbeaver.registry.maven.MavenLocalVersion;
import org.jkiss.dbeaver.registry.maven.MavenRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.net.URLConnection;
import java.text.NumberFormat;

/**
 * DriverFileManager
 */
public class DriverFileManager
{
    static final Log log = Log.getLog(DriverFileManager.class);

    public static void downloadLibraryFile(DBRProgressMonitor monitor, DriverLibraryDescriptor library, boolean updateVersion) throws IOException, InterruptedException
    {
        if (library.isMavenArtifact()) {
            MavenArtifact artifact = downloadMavenArtifact(monitor, library, updateVersion);
            if (artifact.getRepository().isLocal()) {
                // No need to download local artifacts
                return;
            }
        }
        String externalURL = library.getExternalURL();
        if (externalURL == null) {
            throw new IOException("Unresolved file reference: " + library.getPath());
        }

        final URLConnection connection = RuntimeUtils.openConnection(externalURL);
        monitor.worked(1);
        monitor.done();

        final int contentLength = connection.getContentLength();
        monitor.beginTask("Download " + externalURL, contentLength);
        boolean success = false;
        final File localFile = library.getLocalFile();
        if (localFile == null) {
            throw new IOException("No target file for '" + library.getPath() + "'");
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
                    DriverLibraryDescriptor.log.warn("Can't delete local driver file '" + localFile.getAbsolutePath() + "'");
                }
            }
        }
        monitor.done();
    }

    private static MavenArtifact downloadMavenArtifact(DBRProgressMonitor monitor, DriverLibraryDescriptor library, boolean updateVersion) throws IOException {
        MavenArtifactReference artifactInfo = new MavenArtifactReference(library.getPath());
        if (updateVersion) {
            MavenRegistry.getInstance().resetArtifactInfo(artifactInfo);
        }
        MavenArtifact artifact = MavenRegistry.getInstance().findArtifact(artifactInfo);
        if (artifact == null) {
            throw new IOException("Maven artifact '" + library.getPath() + "' not found");
        }
        if (updateVersion) {
            artifact.loadMetadata();
        } else {
            MavenLocalVersion localVersion = artifact.getActiveLocalVersion();
            if (localVersion != null && localVersion.getCacheFile().exists()) {
                // Already cached
                return artifact;
            }
        }
        artifact.resolveVersion(monitor, artifactInfo.getVersion());
        return artifact;
    }
}
