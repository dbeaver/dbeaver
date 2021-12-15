/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.navigator.fs;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DBNPath
 */
public class DBNPath extends DBNPathBase
{
    private static final Log log = Log.getLog(DBNPath.class);

    private static final DBNNode[] EMPTY_NODES = new DBNNode[0];

    private Path path;

    public DBNPath(DBNNode parentNode, Path path) {
        super(parentNode);
        this.path = path;

        DBWorkbench.getPlatform().getDefaultResourceHandler().updateNavigatorNodeFromResource(this, getResource());
    }

    @Override
    public boolean isDisposed() {
        return path == null || super.isDisposed();
    }

    @Override
    protected Path getPath() {
        return path;
    }

    @Override
    protected void dispose(boolean reflect) {
        this.path = null;
        super.dispose(reflect);
    }

    @Override
    public String getNodeDescription() {
        return null;
    }

    @Override
    public boolean allowsChildren() {
        return Files.isDirectory(path);
    }

}
