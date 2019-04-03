/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DatabaseMappingContainer
 */
public class DatabaseMappingContainer implements DatabaseMappingObject {

    private static final Log log = Log.getLog(DatabaseMappingContainer.class);

    private DatabaseConsumerSettings consumerSettings;
    private DBSDataContainer source;
    private DBSDataManipulator target;
    private String targetName;
    private DatabaseMappingType mappingType;
    private List<DatabaseMappingAttribute> attributeMappings = new ArrayList<>();

    public DatabaseMappingContainer(DatabaseConsumerSettings consumerSettings, DBSDataContainer source) {
        this.consumerSettings = consumerSettings;
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    public DatabaseMappingContainer(IRunnableContext context, DatabaseConsumerSettings consumerSettings, DBSDataContainer sourceObject, DBSDataManipulator targetObject) throws DBException {
        this.consumerSettings = consumerSettings;
        this.source = sourceObject;
        this.target = targetObject;
        refreshMappingType(context, DatabaseMappingType.existing);
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
    }

    @Override
    public DatabaseMappingType getMappingType() {
        return mappingType;
    }

    public void refreshMappingType(IRunnableContext context, DatabaseMappingType mappingType) throws DBException {
        this.mappingType = mappingType;
        final Collection<DatabaseMappingAttribute> mappings = getAttributeMappings(context);
        if (!CommonUtils.isEmpty(mappings)) {
            for (DatabaseMappingAttribute attr : mappings) {
                attr.updateMappingType(new VoidProgressMonitor());
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

    @Override
    public String getTargetName() {
        switch (mappingType) {
            case existing:
                return target.getName();
            case create:
                return targetName;
            case skip:
                return DatabaseMappingAttribute.TARGET_NAME_SKIP;
            default:
                return "?";
        }
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public DatabaseMappingAttribute getAttributeMapping(DBSAttributeBase sourceAttr) {
        for (DatabaseMappingAttribute attr : attributeMappings) {
            if (attr.getSource().getName().equalsIgnoreCase(sourceAttr.getName())) {
                return attr;
            }
        }
        return null;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(IRunnableContext runnableContext) {
        if (attributeMappings.isEmpty()) {
            try {
                // Do not use runnable context! It changes active focus and locks UI which breakes whole jface editing framework
                readAttributes(new VoidProgressMonitor());
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Attributes read failed", "Can't get attributes from " + DBUtils.getObjectFullName(source, DBPEvaluationContext.UI), e);
            }
        }
        return attributeMappings;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(DBRProgressMonitor monitor) {
        if (attributeMappings.isEmpty()) {
            try {
                readAttributes(monitor);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Attributes read failed", "Can't get attributes from " + DBUtils.getObjectFullName(source, DBPEvaluationContext.UI), e);
            }
        }
        return attributeMappings;
    }

    private void readAttributes(DBRProgressMonitor monitor) throws DBException {
        if (source instanceof DBSEntity) {
            for (DBSEntityAttribute attr : CommonUtils.safeCollection(((DBSEntity) source).getAttributes(monitor))) {
                if (DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                addAttributeMapping(monitor, attr);
            }
        } else {
            // Seems to be a dynamic query. Execute it to get metadata
            DBPDataSource dataSource = source.getDataSource();
            assert (dataSource != null);
            try (DBCSession session = DBUtils.openUtilSession(monitor, source, "Read query meta data")) {
                MetadataReceiver receiver = new MetadataReceiver();
                source.readData(new AbstractExecutionSource(source, session.getExecutionContext(), this), session, receiver, null, 0, 1, DBSDataContainer.FLAG_NONE, 1);
                for (DBDAttributeBinding attr : receiver.attributes) {
                    if (DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    addAttributeMapping(monitor, attr);
                }
            }
        }
    }

    private void addAttributeMapping(DBRProgressMonitor monitor, DBSAttributeBase attr) throws DBException {
        DatabaseMappingAttribute mapping = new DatabaseMappingAttribute(this, attr);
        mapping.updateMappingType(monitor);
        attributeMappings.add(mapping);
    }

    public void saveSettings(IDialogSettings settings) {
        if (!CommonUtils.isEmpty(targetName)) {
            settings.put("targetName", targetName);
        } else if (target != null) {
            settings.put("targetName", target.getName());
        }
        if (mappingType != null) {
            settings.put("mappingType", mappingType.name());
        }
        if (!attributeMappings.isEmpty()) {
            IDialogSettings attrsSection = settings.addNewSection("attributes");
            for (DatabaseMappingAttribute attrMapping : attributeMappings) {
                DBSAttributeBase sourceAttr = attrMapping.getSource();
                if (sourceAttr != null) {
                    IDialogSettings attrSettings = attrsSection.addNewSection(sourceAttr.getName());
                    attrMapping.saveSettings(attrSettings);
                }
            }
        }
    }

    public void loadSettings(IRunnableContext context, IDialogSettings settings) {
        targetName = settings.get("targetName");
        if (settings.get("mappingType") != null) {
            try {
                DatabaseMappingType newMappingType = DatabaseMappingType.valueOf(settings.get("mappingType"));
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
                refreshMappingType(context, newMappingType);

            } catch (Exception e) {
                log.error(e);
            }
        }
        if (!attributeMappings.isEmpty()) {
            IDialogSettings attrsSection = settings.getSection("attributes");
            if (attrsSection != null) {
                for (DatabaseMappingAttribute attrMapping : attributeMappings) {
                    DBSAttributeBase sourceAttr = attrMapping.getSource();
                    if (sourceAttr != null) {
                        IDialogSettings attrSettings = attrsSection.getSection(sourceAttr.getName());
                        if (attrSettings != null) {
                            attrMapping.loadSettings(attrSettings);
                        }
                    }
                }
            }
        }
    }

    private static class MetadataReceiver implements DBDDataReceiver {

        private List<DBDAttributeBinding> attributes;

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {
            attributes = DBUtils.makeResultAttributeBindings(resultSet);
        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {
        }

        @Override
        public void close() {
        }
    }
}
