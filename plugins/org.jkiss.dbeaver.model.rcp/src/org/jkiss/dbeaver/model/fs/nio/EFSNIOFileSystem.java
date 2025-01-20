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
package org.jkiss.dbeaver.model.fs.nio;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystem;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystemRoot;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * NIOFileSystem
 *
 * URI format:  dbvfs://project-name/fs-type/fs-id/root-id
 *
 */
public class EFSNIOFileSystem extends FileSystem {

    public static final String DBVFS_FS_ID = "dbvfs";

    private static final Log log = Log.getLog(EFSNIOFileSystem.class);

    public EFSNIOFileSystem() {
    }

    @Override
    public IFileStore getStore(URI uri) {
        Path path = null;

        String projectName = CommonUtils.toString(uri.getHost(), uri.getAuthority());
        String[] vfsPath = CommonUtils.removeTrailingSlash(CommonUtils.removeLeadingSlash(uri.getPath()))
            .split("/");
        String relPath = uri.getQuery();
        if (!CommonUtils.isEmpty(projectName) && vfsPath.length == 3 && !CommonUtils.isEmpty(relPath)) {
            DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
            if (project != null) {
                String fsType = vfsPath[0];
                String fsId = vfsPath[1];
                String fsRootPath = vfsPath[2];

                DBNProject projectNode = DBWorkbench.getPlatform().getNavigatorModel().getRoot().getProjectNode(project);
                if (projectNode != null) {
                    DBNFileSystems fileSystemsNode = projectNode.getExtraNode(DBNFileSystems.class);
                    if (fileSystemsNode != null) {
                        try {
                            fileSystemsNode.getChildren(new VoidProgressMonitor());
                            DBNFileSystem fsNode = fileSystemsNode.getFileSystem(fsType, fsId);
                            if (fsNode != null) {
                                fsNode.getChildren(new VoidProgressMonitor());
                                DBNFileSystemRoot fsNodeRoot = fsNode.getRoot(fsRootPath);
                                if (fsNodeRoot != null) {
                                    try {
                                        relPath = CommonUtils.removeLeadingSlash(relPath);
                                        relPath = URLDecoder.decode(relPath, StandardCharsets.UTF_8);
                                        path = fsNodeRoot.getPath().resolve(relPath);
                                    } catch (Exception e) {
                                        throw new IllegalArgumentException("Error resolving path '" + relPath + "'", e);
                                    }
                                }
                            } else {
                                log.debug("File system '" + fsType + ":" + fsId + "' not found");
                            }
                        } catch (Exception e) {
                            if (e instanceof RuntimeException re) {
                                throw re;
                            }
                            throw new IllegalArgumentException("Error reading file systems", e);
                        }
                    }
                }
            }
        }

        if (path == null) {
            throw new IllegalArgumentException("Invalid " + DBVFS_FS_ID + " URI: " + uri);
            //return EFS.getNullFileSystem().getStore(uri);
        }
        return new EFSNIOFileStore(uri, path);
    }

}
