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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOListener;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOResource;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystem;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystemRoot;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.utils.CommonUtils;

public class DBFResourceListener implements EFSNIOListener {

    private static final Log log = Log.getLog(DBFResourceListener.class);

    private final DBNFileSystems fileSystems;

    public DBFResourceListener(DBNFileSystems fileSystems) {
        this.fileSystems = fileSystems;
    }

    @Override
    public void resourceChanged(EFSNIOResource resource, Action action) {
        if (!(fileSystems.getOwnerProject() instanceof RCPProject rcpProject) ||
            !CommonUtils.equalObjects(rcpProject.getEclipseProject(), resource.getProject())) {
            return;
        }
        if (fileSystems.getCachedChildren() == null) {
            return;
        }
        DBFVirtualFileSystemRoot dbfRoot = resource.getRoot().getRoot();

        for (DBNFileSystem fs : fileSystems.getCachedChildren()) {
            if (CommonUtils.equalObjects(fs.getFileSystem(), dbfRoot.getFileSystem())) {
                DBNFileSystemRoot rootNode = fs.getRoot(dbfRoot);
                if (rootNode != null) {
                    String[] pathSegments = fs.getFileSystem().getURISegments(resource.getFileStore().getPath().toUri());
                    //String[] pathSegments = getPathSegments(resource);

                    DBNPathBase parentNode = rootNode;
                    // NIO path format /[config-id]/root-id/folder1/file1
                    for (int i = 1; i < pathSegments.length - 1; i++) {
                        String itemName = pathSegments[i];
                        DBNPathBase childNode = parentNode.getChild(itemName);
                        if (childNode == null) {
                            log.debug("Cannot find child node '" + itemName + "' in '" + parentNode.getNodeUri() + "'");
                            return;
                        }
                        parentNode = childNode;
                    }

                    switch (action) {
                        case CREATE -> parentNode.addChildResource(resource.getNioPath());
                        case DELETE -> parentNode.removeChildResource(resource.getNioPath());
                        default -> {
                        }
                    }
                }
                break;
            }
        }
    }


}
