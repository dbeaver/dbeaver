/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * OracleTableColumn
 */
public class OracleTableColumn extends JDBCTableColumn<OracleTableBase> implements DBSTableColumn, DBPHiddenObject
{
    static final Log log = LogFactory.getLog(OracleTableColumn.class);

    private OracleDataType type;
    private OracleDataTypeModifier typeMod;
    private String comment;
    private String defaultValue;
    private boolean hidden;

    public OracleTableColumn(OracleTableBase table)
    {
        super(table, false);
    }

    public OracleTableColumn(
        DBRProgressMonitor monitor,
        OracleTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);

        this.name = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_ID");
        this.typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.type = OracleDataType.resolveDataType(
            monitor,
            getDataSource(),
            JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"),
            this.typeName);
        this.typeMod = OracleDataTypeModifier.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "DATA_TYPE_MOD"));
        if (this.type != null) {
            this.typeName = type.getName();
            this.valueType = type.getValueType();
        }
        this.maxLength = JDBCUtils.safeGetLong(dbResult, "DATA_LENGTH");
        this.required = !"Y".equals(JDBCUtils.safeGetString(dbResult, "NULLABLE"));
        this.scale = JDBCUtils.safeGetInt(dbResult, "DATA_SCALE");
        this.precision = JDBCUtils.safeGetInt(dbResult, "DATA_PRECISION");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "DATA_DEFAULT");
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
        this.hidden = JDBCUtils.safeGetBoolean(dbResult, "HIDDEN_COLUMN", OracleConstants.YES);
    }

    @Override
    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnDataTypeListProvider.class)
    public DBSDataType getType()
    {
        return type;
    }

    public void setType(OracleDataType type)
    {
        this.type = type;
        this.typeName = type == null ? "" : type.getName();
    }

    @Property(viewable = true, order = 30)
    public OracleDataTypeModifier getTypeMod()
    {
        return typeMod;
    }

    //@Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    @Property(viewable = true, order = 41)
    public int getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, order = 42)
    public int getScale()
    {
        return super.getScale();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public boolean isSequence()
    {
        return false;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 100)
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @Override
    public boolean isHidden()
    {
        return hidden;
    }

    @Override
    public Image getObjectImage()
    {
        if (type != null && type.getName().equals(OracleConstants.TYPE_NAME_XML)) {
            return DBIcon.TYPE_XML.getImage();
        }
        return super.getObjectImage();
    }

    public static class ColumnDataTypeListProvider implements IPropertyValueListProvider<OracleTableColumn> {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(OracleTableColumn column)
        {
            final Collection<? extends DBSDataType> dataTypes = column.getTable().getDataSource().getDataTypes();
            return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
        }
    }

}
