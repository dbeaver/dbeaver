package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;

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
        DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();

        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (objectContainer == null) {
            return;
        }

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

                if (readNodes) {
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
