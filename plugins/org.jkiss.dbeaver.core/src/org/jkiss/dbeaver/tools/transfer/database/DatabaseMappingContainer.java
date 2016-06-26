/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* DatabaseMappingContainer
*/
class DatabaseMappingContainer implements DatabaseMappingObject {
    private DBSDataContainer source;
    private DBSDataManipulator target;
    private String targetName;
    private DatabaseMappingType mappingType;
    private List<DatabaseMappingAttribute> attributeMappings = new ArrayList<>();

    public DatabaseMappingContainer(DBSDataContainer source)
    {
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    public DatabaseMappingContainer(IRunnableContext context, DBSDataContainer sourceObject, DBSDataManipulator targetObject) throws DBException
    {
        this.source = sourceObject;
        this.target = targetObject;
        refreshMappingType(context, DatabaseMappingType.existing);
    }

    @Override
    public DBSDataManipulator getTarget()
    {
        return target;
    }

    public void setTarget(DBSDataManipulator target)
    {
        this.target = target;
    }

    @Override
    public DatabaseMappingType getMappingType()
    {
        return mappingType;
    }

    void refreshMappingType(IRunnableContext context, DatabaseMappingType mappingType) throws DBException
    {
        this.mappingType = mappingType;
        final Collection<DatabaseMappingAttribute> mappings = getAttributeMappings(context);
        if (!CommonUtils.isEmpty(mappings)) {
            for (DatabaseMappingAttribute attr : mappings) {
                attr.updateMappingType(VoidProgressMonitor.INSTANCE);
            }
        }
    }

    void setMappingType(DatabaseMappingType mappingType)
    {
        this.mappingType = mappingType;
    }

    public boolean isCompleted()
    {
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
    public DBPImage getIcon()
    {
        return DBIcon.TREE_TABLE;
    }

    @Override
    public DBSDataContainer getSource()
    {
        return source;
    }

    @Override
    public String getTargetName()
    {
        switch (mappingType) {
            case existing: return DBUtils.getObjectFullName(target);
            case create: return targetName;
            case skip: return DatabaseMappingAttribute.TARGET_NAME_SKIP;
            default: return "?";
        }
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public DatabaseMappingAttribute getAttributeMapping(DBSAttributeBase sourceAttr)
    {
        for (DatabaseMappingAttribute attr : attributeMappings) {
            if (attr.getSource().getName().equalsIgnoreCase(sourceAttr.getName())) {
                return attr;
            }
        }
        return null;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(IRunnableContext runnableContext)
    {
        if (attributeMappings.isEmpty()) {
            try {
                DBeaverUI.run(runnableContext, true, true, new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            readAttributes(monitor);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(null, "Attributes read failed", "Can't get attributes from " + DBUtils.getObjectFullName(source), e.getTargetException());
            } catch (InterruptedException e) {
                // Skip it
            }
        }
        return attributeMappings;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings(DBRProgressMonitor monitor)
    {
        if (attributeMappings.isEmpty()) {
            try {
                readAttributes(monitor);
            } catch (DBException e) {
                UIUtils.showErrorDialog(null, "Attributes read failed", "Can't get attributes from " + DBUtils.getObjectFullName(source), e);
            }
        }
        return attributeMappings;
    }

    private void readAttributes(DBRProgressMonitor monitor) throws DBException
    {
        if (source instanceof DBSEntity) {
            for (DBSEntityAttribute attr : ((DBSEntity) source).getAttributes(monitor)) {
                if (attr.isPseudoAttribute() || DBUtils.isHiddenObject(attr)) {
                    continue;
                }
                addAttributeMapping(monitor, attr);
            }
        } else {
            // Seems to be a dynamic query. Execute it to get metadata
            DBPDataSource dataSource = source.getDataSource();
            assert (dataSource != null);
            try (DBCSession session = DBUtils.openUtilSession(monitor, dataSource, "Read query meta data")) {
                MetadataReceiver receiver = new MetadataReceiver();
                source.readData(new AbstractExecutionSource(source, session.getExecutionContext(), this), session, receiver, null, 0, 1, DBSDataContainer.FLAG_NONE);
                for (DBCAttributeMetaData attr : receiver.attributes) {
                    if (attr.isPseudoAttribute() || DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    addAttributeMapping(monitor, attr);
                }
            }
        }
    }

    private void addAttributeMapping(DBRProgressMonitor monitor, DBSAttributeBase attr) throws DBException
    {
        DatabaseMappingAttribute mapping = new DatabaseMappingAttribute(this, attr);
        mapping.updateMappingType(monitor);
        attributeMappings.add(mapping);
    }

    private static class MetadataReceiver implements DBDDataReceiver {

        private List<DBCAttributeMetaData> attributes;

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException
        {
            attributes = resultSet.getMeta().getAttributes();
        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException
        {
        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException
        {
        }

        @Override
        public void close()
        {
        }
    }
}
