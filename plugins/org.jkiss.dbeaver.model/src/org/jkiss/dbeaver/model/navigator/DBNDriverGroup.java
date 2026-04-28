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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigator node grouping connections that share the same driver.
 * Created when the "Group databases by driver" preference is enabled.
 */
public class DBNDriverGroup extends DBNNode implements DBNContainer {

    private final DBPDriver driver;

    public DBNDriverGroup(@NotNull DBNProjectDatabases parent, @NotNull DBPDriver driver) {
        super(parent);
        this.driver = driver;
    }

    @NotNull
    public DBPDriver getDriver() {
        return driver;
    }

    @NotNull
    @Override
    public String getNodeType() {
        return "driverGroup";
    }

    @NotNull
    @Override
    public String getNodeId() {
        return "driver:" + driver.getId();
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public String getName() {
        return driver.getName();
    }

    @Nullable
    @Override
    public Object getValueObject() {
        return driver;
    }

    @NotNull
    @Override
    public String getChildrenType() {
        return ModelMessages.model_navigator_Connection;
    }

    @Nullable
    @Override
    public Class<?> getChildrenClass() {
        return DBPDataSourceContainer.class;
    }

    @NotNull
    @Override
    public String getNodeDisplayName() {
        return driver.getName();
    }

    @Nullable
    @Override
    public String getNodeDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBPImage getNodeIcon() {
        return driver.getIcon();
    }

    @NotNull
    @Deprecated
    @Override
    public String getNodeItemPath() {
        return getParentNode().getNodeItemPath() + "/driver:" + driver.getId();
    }

    @NotNull
    @Override
    public DBNProjectDatabases getParentNode() {
        return (DBNProjectDatabases) super.getParentNode();
    }

    @Override
    public boolean allowsChildren() {
        return true;
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        return !getDataSources().isEmpty();
    }

    @Nullable
    @Override
    public DBNNode[] getChildren(@NotNull DBRProgressMonitor monitor) {
        List<DBNDataSource> sources = getDataSources();
        sortNodes(sources);
        return sources.toArray(new DBNNode[0]);
    }

    @NotNull
    public List<DBNDataSource> getDataSources() {
        List<DBNDataSource> result = new ArrayList<>();
        for (DBNDataSource ds : getParentNode().getDataSources()) {
            if (ds.getDataSourceContainer().getFolder() == null
                    && ds.getDataSourceContainer().getDriver() == driver) {
                result.add(ds);
            }
        }
        return result;
    }

    public List<DBNDataSource> getNestedDataSources() {
        return getDataSources();
    }

    public boolean hasConnected() {
        for (DBNDataSource ds : getDataSources()) {
            if (ds.getDataSource() != null) {
                return true;
            }
        }
        return false;
    }
}
