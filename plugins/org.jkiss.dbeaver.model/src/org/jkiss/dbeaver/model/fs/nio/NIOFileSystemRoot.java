/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.resources.IProject;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;

/**
 * NIOFileSystemRoot
 */
public class NIOFileSystemRoot {

    private final IProject project;
    private final DBFVirtualFileSystemRoot fsRoot;
    private final String fsPrefix;

    public NIOFileSystemRoot(IProject project, DBFVirtualFileSystemRoot fsRoot, String fsPrefix) {
        this.project = project;
        this.fsRoot = fsRoot;
        this.fsPrefix = fsPrefix;
    }

    public IProject getProject() {
        return project;
    }

    public DBFVirtualFileSystemRoot getRoot() {
        return fsRoot;
    }

    public String getPrefix() {
        return fsPrefix;
    }

}
