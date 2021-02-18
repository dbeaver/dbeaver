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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreForeignTableManager;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreTableColumnManager;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.fdw.FDWConfigDescriptor;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.virtual.DBVContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityForeignKey;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.editors.SimpleCommandContext;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

class PostgreFDWConfigWizard extends BaseWizard implements DBPContextProvider {

    private static final Log log = Log.getLog(PostgreFDWConfigWizard.class);

    private PostgreFDWConfigWizardPageInput inputPage;
    private PostgreFDWConfigWizardPageConfig configPage;
    private PostgreDatabase database;

    private List<DBPDataSourceContainer> availableDataSources = null;
    private List<DBSEntity> proposedEntities = null;
    private List<DBNDatabaseNode> selectedEntities;
    private DBPDataSourceContainer selectedDataSource;
    private PostgreSchema selectedSchema;
    private FDWInfo selectedFDW;
    private String fdwServerId;
    private PropertySourceCustom fdwPropertySource;

    static class FDWInfo {
        PostgreForeignDataWrapper installedFDW;
        FDWConfigDescriptor fdwDescriptor;

        String getId() {
            return installedFDW != null ? installedFDW.getName() : fdwDescriptor.getFdwId();
        }
        String getDescription() {
            return installedFDW != null ? installedFDW.getDescription() : fdwDescriptor.getDescription();
        }
    }

    PostgreFDWConfigWizard(PostgreDatabase database) {
        setWindowTitle("Foreign Data Wrappers configurator");
        this.database = database;
        setNeedsProgressMonitor(true);

        this.fdwPropertySource = new PropertySourceCustom();
    }

    public PostgreDatabase getDatabase() {
        return database;
    }

    public PostgreSchema getSelectedSchema() {
        return selectedSchema;
    }

    public void setSelectedSchema(PostgreSchema selectedSchema) {
        this.selectedSchema = selectedSchema;
    }

    public FDWInfo getSelectedFDW() {
        return selectedFDW;
    }

    public void setSelectedFDW(FDWInfo selectedFDW) {
        this.selectedFDW = selectedFDW;
    }

    public String getFdwServerId() {
        return fdwServerId;
    }

    public void setFdwServerId(String fdwServerId) {
        this.fdwServerId = fdwServerId;
    }

    public PropertySourceCustom getFdwPropertySource() {
        return fdwPropertySource;
    }

    @Override
    public void addPages() {
        inputPage = new PostgreFDWConfigWizardPageInput(this);
        configPage = new PostgreFDWConfigWizardPageConfig(this);
        addPage(inputPage);
        addPage(configPage);
        addPage(new PostgreFDWConfigWizardPageFinal(this));
        super.addPages();
    }

    public List<DBPDataSourceContainer> getAvailableDataSources() {
        return availableDataSources == null ? Collections.emptyList() : availableDataSources;
    }

    public List<DBSEntity> getProposedEntities() {
        return proposedEntities == null ? Collections.emptyList() : proposedEntities;
    }

    public DBPDataSourceContainer getSelectedDataSource() {
        return selectedDataSource;
    }

    public List<DBNDatabaseNode> getSelectedEntities() {
        return selectedEntities == null ? Collections.emptyList() : selectedEntities;
    }

    public void setSelectedEntities(List<DBNDatabaseNode> entities) {
        this.selectedEntities = entities;
        this.selectedDataSource = entities.isEmpty() ? null : entities.get(0).getDataSourceContainer();
    }

    public void addAvailableDataSource(DBPDataSourceContainer dataSource) {
        availableDataSources.add(dataSource);
    }

    public void removeAvailableDataSource(DBPDataSourceContainer dataSource) {
        availableDataSources.remove(dataSource);
    }

    void collectAvailableDataSources(DBRProgressMonitor monitor) {
        if (availableDataSources != null) {
            return;
        }
        Set<DBPDataSourceContainer> dataSources = new LinkedHashSet<>();
        Set<DBSEntity> entities = new LinkedHashSet<>();

        DBPDataSourceContainer curDataSource = database.getDataSource().getContainer();

        // Find all virtual connections
        DBVModel vModel = curDataSource.getVirtualModel();
        monitor.beginTask("Check virtual foreign keys", 1);
        collectAvailableDataSources(monitor, vModel, dataSources, entities);
        monitor.done();

        DBNModel navModel = DBWorkbench.getPlatform().getNavigatorModel();

        // Check global FK references cache
        Map<String, List<DBVEntityForeignKey>> grCache = DBVModel.getGlobalReferenceCache();
        monitor.beginTask("Check external references", grCache.size());
        for (Map.Entry<String, List<DBVEntityForeignKey>> grEntry : grCache.entrySet()) {
            DBNDataSource refDataSource = navModel.getDataSourceByPath(
                database.getDataSource().getContainer().getProject(),
                grEntry.getKey());
            if (refDataSource != null && refDataSource.getDataSourceContainer() == curDataSource) {
                try {
                    for (DBVEntityForeignKey rfk : grEntry.getValue()) {
                        monitor.subTask("Check " + rfk.getEntity().getFullyQualifiedName(DBPEvaluationContext.UI));
                        DBSEntity refEntity = rfk.getEntity().getRealEntity(monitor);
                        if (refEntity != null) {
                            dataSources.add(refEntity.getDataSource().getContainer());
                            entities.add(refEntity);
                        }
                    }
                } catch (DBException e) {
                    log.debug("Error getting referenced entity", e);
                }
            }
            monitor.worked(1);
        }
        monitor.done();

        // Check already configured FDW

        // Done
        availableDataSources = new ArrayList<>(dataSources);
        proposedEntities = new ArrayList<>(entities);
    }

    private void collectAvailableDataSources(DBRProgressMonitor monitor, DBVContainer vContainer, Set<DBPDataSourceContainer> dataSources, Set<DBSEntity> entities) {
        for (DBVContainer childContainer : vContainer.getContainers()) {
            collectAvailableDataSources(monitor, childContainer, dataSources, entities);
        }
        for (DBVEntity vEntity : vContainer.getEntities()) {
            for (DBVEntityForeignKey fk : vEntity.getForeignKeys()) {
                DBPDataSourceContainer dataSource = fk.getAssociatedDataSource();
                if (dataSource != database.getDataSource().getContainer()) {
                    dataSources.add(dataSource);
                    try {
                        entities.add(fk.getAssociatedEntity(monitor));
                    } catch (DBException e) {
                        log.debug("Error getting referenced entity", e);
                    }
                }
            }
        }
    }

    @Override
    public boolean performFinish() {
        try {
            getRunnableContext().run(true, true, monitor -> {
                try {
                    installFDW(monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Error generating FDW", "Error during FDW script execution", e.getTargetException());
                return false;
        } catch (InterruptedException e) {
            return false;
        }

        return true;
    }

    private void installFDW(DBRProgressMonitor monitor) throws DBException {
        monitor.beginTask("Generate FDW script", 2);
        monitor.subTask("Read actions");
        List<DBEPersistAction> actions = generateScript(monitor);
        monitor.subTask("Execute script");
        DBCExecutionContext context = DBUtils.getDefaultContext(getDatabase(), false);
        DBExecUtils.executeScript(monitor, context, "Install FDW", actions);
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return DBUtils.getDefaultContext(database, true);
    }


    List<DBEPersistAction> generateScript(DBRProgressMonitor monitor) throws DBException {
        PostgreDatabase database = getDatabase();
        PostgreDataSource curDataSource = database.getDataSource();
        List<DBEPersistAction> actions = new ArrayList<>();

        PostgreFDWConfigWizard.FDWInfo selectedFDW = getSelectedFDW();
        PropertySourceCustom propertySource = getFdwPropertySource();
        Map<String, Object> propValues = propertySource.getPropertiesWithDefaults();

        String serverId = getFdwServerId();

        actions.add(new SQLDatabasePersistActionComment(curDataSource, "CREATE EXTENSION " + selectedFDW.getId()));
        {
            StringBuilder script = new StringBuilder();
            script.append("CREATE SERVER ").append(serverId)
                .append("\n\tFOREIGN DATA WRAPPER ").append(selectedFDW.getId())
                .append("\n\tOPTIONS(");
            boolean firstProp = true;
            for (Map.Entry<String, Object> pe : propValues.entrySet()) {
                String propName = CommonUtils.toString(pe.getKey());
                String propValue = CommonUtils.toString(pe.getValue());
                if (CommonUtils.isEmpty(propName) || CommonUtils.isEmpty(propValue)) {
                    continue;
                }
                if (!firstProp) script.append(", ");
                script.append(propName).append(" '").append(propValue).append("'");
                firstProp = false;
            }
            script
                .append(")");
            actions.add(new SQLDatabasePersistAction("Create extension", script.toString()));
        }

        actions.add(new SQLDatabasePersistAction("CREATE USER MAPPING FOR CURRENT_USER SERVER " + serverId));

        // Now tables
        DBECommandContext commandContext = new SimpleCommandContext(getExecutionContext(), false);

        try {
            PostgreFDWConfigWizard.FDWInfo fdwInfo = getSelectedFDW();
            Map<String, Object> options = new HashMap<>();
            options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
            PostgreForeignTableManager tableManager = new PostgreForeignTableManager();
            PostgreTableColumnManager columnManager = new PostgreTableColumnManager();

            for (DBNDatabaseNode tableNode : getSelectedEntities()) {
                DBSEntity entity = (DBSEntity) tableNode.getObject();

                PostgreTableForeign pgTable = (PostgreTableForeign) tableManager.createNewObject(monitor, commandContext, getSelectedSchema(), null, options);
                if (pgTable == null) {
                    log.error("Internal error while creating new table");
                    continue;
                }
                pgTable.setName(entity.getName());
                pgTable.setForeignServerName(serverId);
                pgTable.setForeignOptions(new String[0]);

                for (DBSEntityAttribute attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
                    // Cache data types
                    PostgreSchema catalogSchema = database.getCatalogSchema(monitor);
                    if (catalogSchema != null) {
                        catalogSchema.getDataTypes(monitor);
                    }
                    String defTypeName = DBStructUtils.mapTargetDataType(database, attr, true);
                    String plainTargetTypeName = SQLUtils.stripColumnTypeModifiers(defTypeName);
                    PostgreDataType dataType = database.getDataType(monitor, plainTargetTypeName);
                    if (dataType == null) {
                        log.error("Data type '" + plainTargetTypeName + "' not found. Skip column mapping.");
                        continue;
                    }
                    PostgreTableColumn newColumn = columnManager.createNewObject(monitor, commandContext, pgTable, null, options);
                    assert newColumn != null;
                    newColumn.setName(attr.getName());
                    newColumn.setDataType(dataType);
                }

                DBEPersistAction[] tableDDL = tableManager.getTableDDL(monitor, pgTable, options);
                Collections.addAll(actions, tableDDL);
            }
        } finally {
            commandContext.resetChanges(true);
        }

        //CREATE SERVER clickhouse_svr FOREIGN DATA WRAPPER clickhousedb_fdw OPTIONS(dbname 'default', driver '/usr/local/lib/odbc/libclickhouseodbc.so', host '46.101.202.143');
        return actions;
    }


}
