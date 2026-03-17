/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.registry.DBNModelExtenderDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Lazy node provided by extension point.
 */
public class DBNNodeExtension extends DBNNode implements DBPHiddenObject {

    private final DBNModelExtenderDescriptor extender;

    public DBNNodeExtension(@NotNull DBNNode parentNode, @NotNull DBNModelExtenderDescriptor extender) {
        super(parentNode);
        this.extender = extender;
    }

    @NotNull
    @Override
    public String getNodeType() {
        return extender.getNodeType();
    }

    @NotNull
    @Override
    public String getNodeId() {
        return extender.getId();
    }

    @NotNull
    @Override
    public String getNodeDisplayName() {
        return extender.getNodeDisplayName();
    }

    @Nullable
    @Override
    public String getNodeDescription() {
        return extender.getNodeDescription();
    }

    @Nullable
    @Override
    public DBPImage getNodeIcon() {
        return extender.getNodeIcon();
    }

    public <T> boolean matchesType(Class<T> nodeType) {
        return nodeType.getName().equals(this.getNodeType());
    }

    @Nullable
    public DBNNode resolveRealNode() {
        DBNNode extension = createExtension();
        if (extension == null) {
            return null;
        }

        return ((DBNNodeExtendable)getParentNode()).resolveTargetNode(this, extension);
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    protected boolean allowsChildren() {
        return true;
    }

    @Nullable
    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @NotNull
    @Deprecated
    @Override
    public String getNodeItemPath() {
        // Path doesn't include project name
        return NodePathType.ext.getPrefix() + getNodeId();
    }

    @Nullable
    public DBNNode createExtension() {
        return extender.getInstance().createNode(getParentNode());
    }

    @NotNull
    @Override
    public String toString() {
        return getNodeDisplayName();
    }

}