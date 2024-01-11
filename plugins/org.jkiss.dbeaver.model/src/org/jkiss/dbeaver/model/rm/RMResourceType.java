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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;

public class RMResourceType {

    private String id;
    private String displayName;
    private String rootFolder;
    private String icon;
    private String folderIcon;
    private String[] fileExtensions;

    public RMResourceType() {
    }

    public RMResourceType(DBPResourceTypeDescriptor rtd) {
        this.id = rtd.getId();
        this.displayName = rtd.getName();
        this.icon = rtd.getIcon().getLocation();
        this.folderIcon = rtd.getFolderIcon() == null ? null : rtd.getFolderIcon().getLocation();
        this.rootFolder = rtd.getDefaultRoot(null);
        this.fileExtensions = rtd.getFileExtensions();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getFolderIcon() {
        return folderIcon;
    }

    public void setFolderIcon(String folderIcon) {
        this.folderIcon = folderIcon;
    }

    public String[] getFileExtensions() {
        return fileExtensions;
    }

    public void setFileExtensions(String[] fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    @Override
    public String toString() {
        return id;
    }
}
