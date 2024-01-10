/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.utils;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.io.*;
import java.net.URI;

/**
 * Eclipse resource utilities
 */
public class ResourceUtils {

    private static final Log log = Log.getLog(ResourceUtils.class);

    public static void checkFolderExists(IFolder folder)
            throws DBException
    {
        checkFolderExists(folder, new VoidProgressMonitor());
    }

    public static void checkFolderExists(IFolder folder, DBRProgressMonitor monitor)
            throws DBException
    {
        if (!folder.exists()) {
            try {
                folder.create(true, true, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't create folder '" + folder.getFullPath() + "'", e);
            }
        }
    }

    public static long getResourceLastModified(IResource resource) {
        try {
            IFileStore fileStore = EFS.getStore(resource.getLocationURI());
            IFileInfo iFileInfo = fileStore.fetchInfo();
            return iFileInfo.getLastModified();
        } catch (CoreException e) {
            log.debug(e);
            return -1;
        }
    }

    public static long getFileLength(IResource resource) {
        try {
            IFileStore fileStore = EFS.getStore(resource.getLocationURI());
            IFileInfo iFileInfo = fileStore.fetchInfo();
            return iFileInfo.getLength();
        } catch (CoreException e) {
            log.debug(e);
            return -1;
        }
    }

    public static void syncFile(DBRProgressMonitor monitor, IResource localFile) {
        // Sync file with contents
        try {
            localFile.refreshLocal(IFile.DEPTH_ZERO, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Can't synchronize file '" + localFile + "' with contents", e);
        }
    }

    public static IFile getUniqueFile(IFolder folder, String fileName, String fileExt)
    {
        IFile file = folder.getFile(fileName + "." + fileExt);
        int index = 1;
        while (file.exists()) {
            file = folder.getFile(fileName + "-" + index + "." + fileExt);
            index++;
        }
        return file;
    }

    @Nullable
    public static IFile convertPathToWorkspaceFile(IPath path)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IFile file = root.getFileForLocation(path);
        if (file != null) {
            return file;
        }
        // Probably we have a path to some linked resource
        IPath folderPath = path.removeLastSegments(1);
        URI folderURI = folderPath.toFile().toURI();
        IContainer[] containers = root.findContainersForLocationURI(folderURI);
        if (!ArrayUtils.isEmpty(containers)) {
            IContainer container = containers[0];
            file = container.getFile(path.removeFirstSegments(path.segmentCount() - 1));
        }
        return file;
    }

    @Nullable
    public static IPath convertPathToWorkspacePath(IPath path)
    {
        IFile wFile = convertPathToWorkspaceFile(path);
        return wFile == null ? null : wFile.getFullPath();
    }

    public static void deleteTempFile(DBRProgressMonitor monitor, IFile file)
    {
        try {
            file.delete(true, false, monitor.getNestedMonitor());
        }
        catch (CoreException e) {
            log.warn("Can't delete temporary file '" + file.getFullPath().toString() + "'", e);
        }
    }

    public static void copyStreamToFile(DBRProgressMonitor monitor, InputStream inputStream, long contentLength, IFile localFile)
        throws IOException
    {
        //localFile.appendContents(inputStream, true, false, monitor.getNestedMonitor());
        File file = localFile.getLocation().toFile();
        try {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                ContentUtils.copyStreams(inputStream, contentLength, outputStream, monitor);
            }
        }
        finally {
            inputStream.close();
        }
        syncFile(monitor, localFile);

    }

    public static void copyReaderToFile(DBRProgressMonitor monitor, Reader reader, long contentLength, String charset, IFile localFile)
        throws IOException
    {
        try {
            if (charset == null) {
                charset = localFile.getCharset();
            } else {
                localFile.setCharset(charset, monitor.getNestedMonitor());
            }
        }
        catch (CoreException e) {
            log.warn("Can't set content charset", e);
        }
        File file = localFile.getLocation().toFile();
        try {
            try (OutputStream outputStream = new FileOutputStream(file)) {
                Writer writer = new OutputStreamWriter(outputStream, charset == null ? GeneralUtils.DEFAULT_ENCODING : charset);
                ContentUtils.copyStreams(reader, contentLength, writer, monitor);
                writer.flush();
            }
        }
        finally {
            reader.close();
        }
        syncFile(monitor, localFile);
    }

}
