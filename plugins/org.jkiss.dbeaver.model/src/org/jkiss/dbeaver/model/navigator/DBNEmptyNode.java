/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Empty node
 */
public class DBNEmptyNode extends DBNNode
{
    public DBNEmptyNode()
    {
        super();
    }

    @NotNull
    @Override
    public String getNodeType()
    {
        return "empty";
    }

    @NotNull
    @Override
    public String getNodeDisplayName()
    {
        return "#empty"; //$NON-NLS-1$
    }

    @Nullable
    @Override
    public String getNodeDescription()
    {
        return "Empty";
    }

    @Nullable
    @Override
    public DBPImage getNodeIcon()
    {
        return null;
    }

    @Override
    public boolean allowsChildren()
    {
        return false;
    }

    @NotNull
    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor)
    {
        return null;
    }

    @Override
    public boolean allowsOpen()
    {
        return false;
    }

    @NotNull
    @Deprecated
    @Override
    public String getNodeItemPath() {
        return null;
    }

}
