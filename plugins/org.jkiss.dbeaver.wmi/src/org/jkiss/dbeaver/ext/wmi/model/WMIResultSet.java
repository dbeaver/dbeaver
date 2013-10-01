/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.utils.CommonUtils;
import org.jkiss.wmi.service.*;

import java.util.*;

/**
 * WMI result set
 */
public class WMIResultSet implements DBCResultSet, DBCResultSetMetaData, DBCEntityMetaData
{
    private DBCExecutionContext context;
    private WMIClass classObject;
    private Collection<WMIObject> rows;
    private Iterator<WMIObject> iterator;
    private WMIObject row;
    private List<DBCAttributeMetaData> properties;

    public WMIResultSet(DBCExecutionContext context, WMIClass classObject, Collection<WMIObject> rows) throws WMIException
    {
        this.context = context;
        this.classObject = classObject;
        this.rows = rows;
        this.iterator = rows.iterator();
        this.row = null;
        {
            // Init meta properties
            WMIObject metaObject;
            if (classObject != null) {
                metaObject = classObject.getClassObject();
            } else if (!rows.isEmpty()) {
                metaObject = rows.iterator().next();
            } else {
                metaObject = null;
            }
            if (metaObject == null) {
                properties = Collections.emptyList();
            } else {
                Collection<WMIObjectAttribute> props = metaObject.getAttributes(WMIConstants.WBEM_FLAG_ALWAYS);
                properties = new ArrayList<DBCAttributeMetaData>(props.size());
                int index = 0;
                for (WMIObjectAttribute prop : props) {
                    if (!prop.isSystem()) {
                        properties.add(new MetaProperty(prop, index++));
                    }
                }
            }
        }

    }

    @Override
    public DBCExecutionContext getContext()
    {
        return context;
    }

    @Override
    public DBCStatement getSource()
    {
        return null;
    }

    @Override
    public Object getColumnValue(int index) throws DBCException
    {
        try {
            if (index > properties.size()) {
                throw new DBCException("Column index " + index + " out of bounds (" + properties.size() + ")");
            }
            return row.getValue(properties.get(index - 1).getName());
        } catch (WMIException e) {
            throw new DBCException(e);
        }
    }

    @Override
    public Object getColumnValue(String name) throws DBCException
    {
        try {
            return row.getValue(name);
        } catch (WMIException e) {
            throw new DBCException(e);
        }
    }

    @Override
    public DBDValueMeta getColumnValueMeta(int index) throws DBCException
    {
        return null;
    }

    @Override
    public DBDValueMeta getRowMeta() throws DBCException
    {
        try {
            Collection<WMIQualifier> qualifiers = row.getQualifiers();
            if (CommonUtils.isEmpty(qualifiers)) {
                return null;
            }
            return new WMIValueMeta(qualifiers);
        } catch (WMIException e) {
            throw new DBCException("Can't read value qualifiers");
        }
    }

    @Override
    public boolean nextRow() throws DBCException
    {
        if (!this.iterator.hasNext()) {
            return false;
        }
        row = iterator.next();
        return true;
    }

    @Override
    public boolean moveTo(int position) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    @Override
    public DBCResultSetMetaData getResultSetMetaData() throws DBCException
    {
        return this;
    }

    @Override
    public void close()
    {
        for (WMIObject row : rows) {
            row.release();
        }
        rows.clear();
        row = null;
    }

    /////////////////////////////////////////////////////////////
    // DBCResultSetMetaData

    @Override
    public List<DBCAttributeMetaData> getAttributes()
    {
        return properties;
    }

    /////////////////////////////////////////////////////////////
    // DBCTableMetaData

    @Override
    public DBSEntity getEntity(DBRProgressMonitor monitor) throws DBException
    {
        return classObject == null ? null : classObject;
    }

    @Override
    public String getEntityName()
    {
        return classObject == null ? null : classObject.getName();
    }

    @Override
    public String getEntityAlias()
    {
        return null;
    }

    @Override
    public boolean isIdentified(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    @Override
    public DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public DBCAttributeMetaData getColumnMetaData(DBRProgressMonitor monitor, DBSEntityAttribute column) throws DBException
    {
        for (DBCAttributeMetaData cmd : properties) {
            if (cmd.getAttribute(monitor) == column) {
                return cmd;
            }
        }
        return null;
    }

    /////////////////////////////////////////////////////////////
    // Meta property

    private class MetaProperty implements DBCAttributeMetaData,IObjectImageProvider
    {
        private final WMIObjectAttribute attribute;
        private final int index;

        private MetaProperty(WMIObjectAttribute attribute, int index)
        {
            this.attribute = attribute;
            this.index = index;
        }

        @Override
        public String getName()
        {
            return attribute.getName();
        }

        @Override
        public long getMaxLength()
        {
            return 0;
        }

        @Override
        public String getTypeName()
        {
            return attribute.getTypeName();
        }

        @Override
        public int getTypeID()
        {
            return attribute.getType();
        }

        @Override
        public DBPDataKind getDataKind()
        {
            return WMIClassAttribute.getDataKindById(attribute.getType());
        }

        @Override
        public int getScale()
        {
            return 0;
        }

        @Override
        public int getPrecision()
        {
            return 0;
        }

        @Override
        public int getIndex()
        {
            return index;
        }

        @Override
        public String getLabel()
        {
            return attribute.getName();
        }

        @Override
        public String getEntityName()
        {
            return classObject == null ? null : classObject.getName();
        }

        @Override
        public String getCatalogName()
        {
            return null;
        }

        @Override
        public String getSchemaName()
        {
            return classObject == null ? null : classObject.getNamespace().getName();
        }

        @Override
        public boolean isReadOnly()
        {
            return false;
        }

        @Override
        public DBDPseudoAttribute getPseudoAttribute()
        {
            return null;
        }

        @Override
        public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor) throws DBException
        {
            return classObject == null ? null : classObject.getAttribute(monitor, getName());
        }

        @Override
        public DBCEntityMetaData getEntity()
        {
            return WMIResultSet.this;
        }

        @Override
        public boolean isReference(DBRProgressMonitor monitor) throws DBException
        {
            return false;
        }

        @Override
        public List<DBSEntityReferrer> getReferrers(DBRProgressMonitor monitor) throws DBException
        {
            return null;
        }

        @Override
        public Image getObjectImage()
        {
            return WMIClassAttribute.getPropertyImage(attribute.getType());
        }

        @Override
        public boolean isRequired()
        {
            return false;
        }
    }

}
