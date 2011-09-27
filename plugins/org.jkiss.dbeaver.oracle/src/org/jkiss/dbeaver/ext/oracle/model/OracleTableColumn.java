/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSHiddenObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.DBIcon;

import java.sql.ResultSet;

/**
 * OracleTableColumn
 */
public class OracleTableColumn extends JDBCTableColumn<OracleTableBase> implements DBSTableColumn, DBSHiddenObject
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
        this.notNull = !"Y".equals(JDBCUtils.safeGetString(dbResult, "NULLABLE"));
        this.scale = JDBCUtils.safeGetInt(dbResult, "DATA_SCALE");
        this.precision = JDBCUtils.safeGetInt(dbResult, "DATA_PRECISION");
        this.defaultValue = JDBCUtils.safeGetString(dbResult, "DATA_DEFAULT");
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
        this.hidden = JDBCUtils.safeGetBoolean(dbResult, "HIDDEN_COLUMN", OracleConstants.YES);
    }

    public DBSObject getParentObject()
    {
        return getTable();
    }

    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, description = "Datatype of the column")
    public DBSDataType getType()
    {
        return type;
    }

    @Property(name = "Type Mod", viewable = true, order = 30, description = "Datatype modifier of the column")
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

    @Property(name = "Length", viewable = true, editable = true, updatable = true, order = 40, description = "Length of the column (in bytes)")
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Property(name = "Precision", viewable = true, order = 41, description = "Decimal precision for NUMBER datatype; binary precision for FLOAT datatype; NULL for all other datatypes")
    public int getPrecision()
    {
        return super.getPrecision();
    }

    @Property(name = "Scale", viewable = true, order = 42, description = "Digits to the right of the decimal point in a number")
    public int getScale()
    {
        return super.getScale();
    }

    @Property(name = "Not Null", viewable = true, editable = true, updatable = true, order = 50, description = "Indicates whether a column allows NULLs")
    @Override
    public boolean isNotNull()
    {
        return super.isNotNull();
    }

    @Property(name = "Default", viewable = true, editable = true, updatable = true, order = 70, description = "Default value for the column")
    public String getDefaultValue()
    {
        return defaultValue;
    }

    public boolean isAutoIncrement()
    {
        return false;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Property(name = "Comment", viewable = true, editable = true, updatable = true, order = 100, description = "Column comment")
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public Image getObjectImage()
    {
        if (type != null && type.getName().equals(OracleConstants.TYPE_NAME_XML)) {
            return DBIcon.TYPE_XML.getImage();
        }
        return super.getObjectImage();
    }

}
