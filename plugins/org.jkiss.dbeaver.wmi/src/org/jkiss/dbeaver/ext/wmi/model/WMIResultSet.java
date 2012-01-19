/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.wmi.service.WMIConstants;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIObject;
import org.jkiss.wmi.service.WMIObjectAttribute;

import java.util.*;

/**
 * WMI result set
 */
public class WMIResultSet implements DBCResultSet, DBCResultSetMetaData, DBCTableMetaData
{
    private DBCExecutionContext context;
    private WMIClass classObject;
    private Collection<WMIObject> rows;
    private Iterator<WMIObject> iterator;
    private WMIObject row;
    private List<DBCColumnMetaData> properties;

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
                properties = new ArrayList<DBCColumnMetaData>(props.size());
                int index = 0;
                for (WMIObjectAttribute prop : props) {
                    if (!prop.isSystem()) {
                        properties.add(new MetaProperty(prop, index++));
                    }
                }
            }
        }

    }

    public DBCExecutionContext getContext()
    {
        return context;
    }

    public DBCStatement getSource()
    {
        return null;
    }

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

    public Object getColumnValue(String name) throws DBCException
    {
        try {
            return row.getValue(name);
        } catch (WMIException e) {
            throw new DBCException(e);
        }
    }

    public boolean nextRow() throws DBCException
    {
        if (!this.iterator.hasNext()) {
            return false;
        }
        row = iterator.next();
        return true;
    }

    public boolean moveTo(int position) throws DBCException
    {
        throw new DBCException("Not Implemented");
    }

    public DBCResultSetMetaData getResultSetMetaData() throws DBCException
    {
        return this;
    }

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

    public DBCResultSet getResultSet()
    {
        return this;
    }

    public List<DBCColumnMetaData> getColumns()
    {
        return properties;
    }

    /////////////////////////////////////////////////////////////
    // DBCTableMetaData

    public DBSTable getTable(DBRProgressMonitor monitor) throws DBException
    {
        return classObject == null ? null : classObject;
    }

    public String getTableName()
    {
        return classObject == null ? null : classObject.getName();
    }

    public String getTableAlias()
    {
        return null;
    }

    public boolean isIdentified(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

    public DBCTableIdentifier getBestIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    /////////////////////////////////////////////////////////////
    // Meta property

    private class MetaProperty implements DBCColumnMetaData,IObjectImageProvider
    {
        private final WMIObjectAttribute attribute;
        private final int index;

        private MetaProperty(WMIObjectAttribute attribute, int index)
        {
            this.attribute = attribute;
            this.index = index;
        }

        public String getName()
        {
            return attribute.getName();
        }

        public boolean isNotNull()
        {
            return false;
        }

        public long getMaxLength()
        {
            return 0;
        }

        public String getTypeName()
        {
            return attribute.getTypeName();
        }

        public int getTypeID()
        {
            return attribute.getType();
        }

        public int getScale()
        {
            return 0;
        }

        public int getPrecision()
        {
            return 0;
        }

        public int getIndex()
        {
            return index;
        }

        public String getLabel()
        {
            return attribute.getName();
        }

        public String getTableName()
        {
            return classObject == null ? null : classObject.getName();
        }

        public String getCatalogName()
        {
            return null;
        }

        public String getSchemaName()
        {
            return classObject == null ? null : classObject.getNamespace().getName();
        }

        public boolean isReadOnly()
        {
            return false;
        }

        public boolean isWritable()
        {
            return false;
        }

        public DBSTableColumn getTableColumn(DBRProgressMonitor monitor) throws DBException
        {
            return classObject == null ? null : classObject.getColumn(monitor, getName());
        }

        public DBCTableMetaData getTable()
        {
            return null;
        }

        public boolean isForeignKey(DBRProgressMonitor monitor) throws DBException
        {
            return false;
        }

        public List<DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor) throws DBException
        {
            return null;
        }

        public Image getObjectImage()
        {
            return WMIClassAttribute.getPropertyImage(attribute.getType());
        }
    }

}
