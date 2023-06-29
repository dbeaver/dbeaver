/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ContextDefaultObjectsReader implements DBRRunnableWithProgress {
    private final DBPDataSource dataSource;
    private final DBCExecutionContext executionContext;
    private final List<DBNDatabaseNode> nodeList = new ArrayList<>();
    // Remote instance node
    private DBSObject defaultObject;
    private boolean enabled;
    private boolean readNodes = false;
    private String currentDatabaseInstanceName;
    private Collection<? extends DBSObject> objectList;

    public ContextDefaultObjectsReader(DBPDataSource dataSource, DBCExecutionContext executionContext) {
        this.dataSource = dataSource;
        this.executionContext = executionContext;
    }

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    public String getDefaultCatalogName() {
        return currentDatabaseInstanceName;
    }

    public DBSObject getDefaultObject() {
        return defaultObject;
    }

    public Collection<? extends DBSObject> getObjectList() {
        return objectList;
    }

    public void setReadNodes(boolean readNodes) {
        this.readNodes = readNodes;
    }

    public List<DBNDatabaseNode> getNodeList() {
        return nodeList;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (objectContainer == null) {
            return;
        }

        DBNModel navigatorModel = DBNUtils.getNavigatorModel(objectContainer);

        DBCExecutionContextDefaults contextDefaults = null;
        if (executionContext != null) {
            contextDefaults = executionContext.getContextDefaults();
        }
        if (contextDefaults == null) {
            return;
        }
        try {
            monitor.beginTask("Read default objects", 1);
            currentDatabaseInstanceName = null;

            Class<? extends DBSObject> childType = objectContainer.getPrimaryChildType(monitor);
            if (childType == null || !DBSObjectContainer.class.isAssignableFrom(childType)) {
                enabled = false;
            } else {
                enabled = true;

                DBSObjectContainer defObject = null;
                if (DBSCatalog.class.isAssignableFrom(childType)) {
                    defObject = contextDefaults.getDefaultCatalog();
                }
                if (defObject != null) {
                    Class<? extends DBSObject> catalogChildrenType = defObject.getPrimaryChildType(monitor);
                    if (catalogChildrenType != null && DBSSchema.class.isAssignableFrom(catalogChildrenType)) {
                        currentDatabaseInstanceName = defObject.getName();
                        if (contextDefaults.supportsSchemaChange()) {
                            objectContainer = defObject;
                        } else if (!contextDefaults.supportsCatalogChange()) {
                            // Nothing can be changed
                            objectContainer = null;
                        }
                        DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                        if (defaultSchema != null) {
                            defObject = defaultSchema;
                        }
                    }
                }
                objectList = objectContainer == null ?
                    (defObject == null ? Collections.emptyList() : Collections.singletonList(defObject)) :
                    objectContainer.getChildren(monitor);
                defaultObject = defObject;

                if (readNodes && navigatorModel != null) {
                    // Cache navigator nodes
                    if (objectList != null) {
                        for (DBSObject child : objectList) {
                            if (DBUtils.getAdapter(DBSObjectContainer.class, child) != null) {
                                DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, child, false);
                                if (node != null) {
                                    nodeList.add(node);
                                }
                            }
                        }
                    }
                }
            }
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        } finally {
            monitor.done();
        }
    }
}
