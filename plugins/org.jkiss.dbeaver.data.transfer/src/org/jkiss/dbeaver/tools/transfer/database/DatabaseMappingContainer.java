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
package org.jkiss.dbeaver.tools.transfer.database;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Select;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DatabaseMappingContainer
 */
public class DatabaseMappingContainer implements DatabaseMappingObject {

    private static final Log log = Log.getLog(DatabaseMappingContainer.class);

    private final DatabaseConsumerSettings consumerSettings;
    private DBSDataContainer source;
    private DBSDataManipulator target;
    private String targetName;
    private DatabaseMappingType mappingType;
    private final List<DatabaseMappingAttribute> attributeMappings = new ArrayList<>();
    private Map<DBPPropertyDescriptor, Object> changedPropertiesMap;
    private Map<String, Object> rawChangedPropertiesMap; // For tasks with empty container

    public DatabaseMappingContainer(DatabaseConsumerSettings consumerSettings, DBSDataContainer source) {
        this.consumerSettings = consumerSettings;
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    public DatabaseMappingContainer(DBRProgressMonitor monitor, DatabaseConsumerSettings consumerSettings, DBSDataContainer sourceObject, DBSDataManipulator targetObject) throws DBException {
        this.consumerSettings = consumerSettings;
        this.source = sourceObject;
        this.target = targetObject;
        refreshMappingType(monitor, DatabaseMappingType.existing, false, false);
    }

    public DatabaseMappingContainer(DatabaseMappingContainer container, DBSDataContainer sourceObject) {
        this.consumerSettings = container.consumerSettings;
        this.source = sourceObject;
        this.target = container.target;
        this.targetName = container.targetName;
        this.mappingType = container.mappingType;
        for (DatabaseMappingAttribute attribute : container.attributeMappings) {
            this.attributeMappings.add(new DatabaseMappingAttribute(attribute, this));
        }
    }

    public DatabaseConsumerSettings getSettings() {
        return consumerSettings;
    }

    @Override
    public DBSDataManipulator getTarget() {
        return target;
    }

    public void setTarget(DBSDataManipulator target) {
        this.target = target;
        this.targetName = null;
    }

    @Override
    public DatabaseMappingType getMappingType() {
        return mappingType;
    }

    public void refreshMappingType(DBRRunnableContext context, DatabaseMappingType mappingType, boolean forceRefresh) throws DBException {
        refreshMappingType(new VoidProgressMonitor(), mappingType, forceRefresh, false);
    }

    public void refreshMappingType(
        DBRProgressMonitor monitor,
        DatabaseMappingType mappingType,
        boolean forceRefresh,
        boolean updateAttributesNames
    ) throws DBException {
        this.mappingType = mappingType;
        refreshAttributesMappingTypes(monitor, forceRefresh, updateAttributesNames);
    }

    public void refreshAttributesMappingTypes(
        DBRProgressMonitor monitor,
        boolean forceRefresh,
        boolean updateAttributesNames
    ) throws DBException {
        final Collection<DatabaseMappingAttribute> mappings = getAttributeMappings(monitor);
        if (!CommonUtils.isEmpty(mappings)) {
            for (DatabaseMappingAttribute attr : mappings) {
                attr.updateMappingType(monitor, forceRefresh, updateAttributesNames);
            }
        }
    }

    void setMappingType(DatabaseMappingType mappingType) {
        this.mappingType = mappingType;
    }

    public boolean isCompleted() {
        if (mappingType == DatabaseMappingType.skip) {
            return true;
        }
        for (DatabaseMappingAttribute attr : attributeMappings) {
            if (attr.getMappingType() == DatabaseMappingType.unspecified) {
                return false;
            }
        }
        return true;
    }

    @Override
    public DBPImage getIcon() {
        return DBIcon.TREE_TABLE;
    }

    @Override
    public DBSDataContainer getSource() {
        return source;
    }

    public Map<DBPPropertyDescriptor, Object> getChangedPropertiesMap() {
        return changedPropertiesMap;
    }

    public void setChangedPropertiesMap(Map<DBPPropertyDescriptor, Object> changedPropertiesMap) {
        this.changedPropertiesMap = changedPropertiesMap;
    }

    public Map<String, Object> getRawChangedPropertiesMap() {
        return rawChangedPropertiesMap;
    }

    @Override
    public String getTargetName() {
        String targetTableName = targetName;
        if (CommonUtils.isEmpty(targetTableName)) {
            if (target != null) {
                targetTableName = target.getName();
            } else {
                DBSDataContainer theSource = this.source;
                if (theSource != null) {
                    if (theSource instanceof IAdaptable) {
                        DBSDataContainer adapterSource = ((IAdaptable) theSource).getAdapter(DBSDataContainer.class);
                        if (adapterSource != null) {
                            theSource = adapterSource;
                        }
                    }
                    if (theSource instanceof SQLQueryContainer) {
                        final SQLQueryContainer sqlQueryContainer = (SQLQueryContainer) theSource;
                        if (sqlQueryContainer.getQuery() instanceof SQLQuery) {
                            final SQLQuery sqlQuery = (SQLQuery) sqlQueryContainer.getQuery();
                            if (sqlQuery.getStatement() instanceof Select) {
                                final Table table = SQLSemanticProcessor.getTableFromSelect((Select) sqlQuery.getStatement());
                                if (table != null) {
                                    targetTableName = table.getName();
                                }
                            }
                        }
                    }
                    if (CommonUtils.isEmpty(targetTableName)) {
                        targetTableName = theSource.getName();
                    }
                } else {
                    targetTableName = "";
                }
            }
        }
        switch (mappingType) {
            case existing:
            case recreate:
                if (target != null) {
                    return target.getName();
                }
                return targetTableName;
            case skip:
                return DatabaseMappingAttribute.TARGET_NAME_SKIP;
            case create:
            default:
                return targetTableName;
        }
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    DatabaseMappingAttribute getAttributeMapping(@NotNull DBDAttributeBinding sourceAttr) {
        return CommonUtils.findBestCaseAwareMatch(
            attributeMappings,
            CommonUtils.notNull(sourceAttr.getLabel(), sourceAttr.getName()), attr -> attr.getSourceLabelOrName(attr.getSource()));
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(DBRRunnableContext runnableContext) {
        if (attributeMappings.isEmpty()) {
            try {
                // Do not use runnable context! It changes active focus and locks UI which breakes whole jface editing framework
                readAttributes(new VoidProgressMonitor());
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(DTMessages.database_mapping_container_title_attributes_read_failed,
                        NLS.bind(DTMessages.database_mapping_container_message_get_attributes_from, DBUtils.getObjectFullName(source, DBPEvaluationContext.UI)), e);
            }
        }
        return attributeMappings;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(DBRProgressMonitor monitor) {
        if (attributeMappings.isEmpty()) {
            try {
                readAttributes(monitor);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(DTMessages.database_mapping_container_title_attributes_read_failed,
                        NLS.bind(DTMessages.database_mapping_container_message_get_attributes_from, DBUtils.getObjectFullName(source, DBPEvaluationContext.UI)), e);
            }
        }
        return attributeMappings;
    }

    private void readAttributes(DBRProgressMonitor monitor) throws DBException {
        for (DBSAttributeBase attr : DTUtils.getAttributes(monitor, source, this)) {
            addAttributeMapping(monitor, attr);
        }
    }

    private void addAttributeMapping(DBRProgressMonitor monitor, DBSAttributeBase attr) throws DBException {
        DatabaseMappingAttribute mapping = new DatabaseMappingAttribute(this, attr);
        mapping.updateMappingType(monitor, false, false);
        attributeMappings.add(mapping);
    }

    public void saveSettings(Map<String, Object> settings) {
        if (!CommonUtils.isEmpty(targetName)) {
            settings.put("targetName", targetName);
        } else if (target != null) {
            settings.put("targetName", target.getName());
        }
        if (mappingType != null) {
            settings.put("mappingType", mappingType.name());
        }
        if (!attributeMappings.isEmpty()) {
            Map<String, Object> attrsSection = new LinkedHashMap<>();
            settings.put("attributes", attrsSection);
            for (DatabaseMappingAttribute attrMapping : attributeMappings) {
                DBSAttributeBase sourceAttr = attrMapping.getSource();
                if (sourceAttr != null) {
                    Map<String, Object> attrSettings = new LinkedHashMap<>();
                    attrsSection.put(attrMapping.getSourceLabelOrName(sourceAttr), attrSettings);
                    attrMapping.saveSettings(attrSettings);
                }
            }
        }
        if (!CommonUtils.isEmpty(changedPropertiesMap)) {
            Map<String, Object> propertiesMap = new LinkedHashMap<>();
            settings.put("changedProperties", propertiesMap);
            for (Map.Entry<DBPPropertyDescriptor, Object> entry : changedPropertiesMap.entrySet()) {
                Object value = entry.getValue();
                propertiesMap.put(entry.getKey().getId(), value.toString());
            }
        } else if (!CommonUtils.isEmpty(rawChangedPropertiesMap)) {
            // In case then we have only the raw map of changed container properties
            settings.put("changedProperties", rawChangedPropertiesMap);
        }
    }

    public void loadSettings(DBRRunnableContext context, Map<String, Object> settings) {
        targetName = CommonUtils.toString(settings.get("targetName"), targetName);
        if (settings.get("mappingType") != null) {
            try {
                DatabaseMappingType newMappingType = DatabaseMappingType.valueOf((String) settings.get("mappingType"));
                if (!CommonUtils.isEmpty(targetName)) {
                    DBSObjectContainer objectContainer = consumerSettings.getContainer();
                    if (objectContainer != null) {
                        DBSObject child = objectContainer.getChild(new VoidProgressMonitor(), targetName);
                        if (child instanceof DBSDataManipulator) {
                            target = (DBSDataManipulator) child;
                        }
                    }
                }

                if (target != null && newMappingType == DatabaseMappingType.create) {
                    // Change create to existing.
                    newMappingType = DatabaseMappingType.existing;
                } else if (target == null && newMappingType == DatabaseMappingType.existing) {
                    newMappingType = DatabaseMappingType.create;
                }
                refreshMappingType(context, newMappingType, false);

            } catch (Exception e) {
                log.error(e);
            }
        }
        if (!attributeMappings.isEmpty()) {
            Map<String, Object> attrsSection = JSONUtils.getObject(settings, "attributes");
            if (!attrsSection.isEmpty()) {
                for (DatabaseMappingAttribute attrMapping : attributeMappings) {
                    DBSAttributeBase sourceAttr = attrMapping.getSource();
                    if (sourceAttr != null) {
                        Map<String, Object> attrSettings = JSONUtils.getObject(attrsSection, attrMapping.getSourceLabelOrName(sourceAttr));
                        if (!attrSettings.isEmpty()) {
                            attrMapping.loadSettings(attrSettings);
                        }
                    }
                }
            }
        }
        rawChangedPropertiesMap = JSONUtils.getObject(settings, "changedProperties");
    }

    public boolean isSameMapping(DatabaseMappingContainer mapping) {
        if (!CommonUtils.equalObjects(source, mapping.source) ||
            attributeMappings.size() != mapping.attributeMappings.size()) {
            return false;
        }
        for (int i = 0; i < attributeMappings.size(); i++) {
            if (!CommonUtils.equalObjects(
                attributeMappings.get(i).getSource().getName(),
                mapping.attributeMappings.get(i).getSource().getName())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasNewTargetObject() {
        return mappingType == DatabaseMappingType.create || mappingType == DatabaseMappingType.recreate;
    }

    public String getTargetFullName() {
        DBSObjectContainer container = consumerSettings.getContainer();

        if (container instanceof DBSSchema || container instanceof DBSCatalog) {
            return DBUtils.getObjectFullName(container, DBPEvaluationContext.DML) + "." + targetName;
        }
        return targetName;
    }
}
