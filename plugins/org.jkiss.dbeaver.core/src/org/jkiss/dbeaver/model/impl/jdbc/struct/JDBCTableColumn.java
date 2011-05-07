/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.Set;
import java.util.TreeSet;

/**
 * JDBC abstract table column
 */
public abstract class JDBCTableColumn<TABLE_TYPE extends JDBCTable> extends JDBCColumn {

    private final TABLE_TYPE table;
    private boolean persisted;

    protected JDBCTableColumn(TABLE_TYPE table, boolean persisted)
    {
        this.table = table;
        this.persisted = persisted;
    }

    protected JDBCTableColumn(TABLE_TYPE table, boolean persisted, String name, String typeName, int valueType, int ordinalPosition, long maxLength, int scale, int radix, int precision, boolean nullable, String description)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, radix, precision, nullable, description);
        this.table = table;
        this.persisted = persisted;
    }

    public TABLE_TYPE getTable()
    {
        return table;
    }

    @Property(name = "Column Name", viewable = true, editable = true, order = 10)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Data Type", viewable = true, editable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    @Property(name = "Length", viewable = true, editable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Property(name = "Not Null", viewable = true, editable = true, order = 50)
    @Override
    public boolean isNotNull()
    {
        return super.isNotNull();
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
        getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, this));
    }

    public static class ColumnTypeNameListProvider implements IPropertyValueListProvider {

        public boolean allowCustomValue()
        {
            return true;
        }

        public Object[] getPossibleValues(Object object)
        {
            Set<String> typeNames = new TreeSet<String>();
            JDBCTableColumn column = (JDBCTableColumn) object;
            for (DBSDataType type : column.getDataSource().getInfo().getSupportedDataTypes()) {
                if (type.getDataKind() != DBSDataKind.UNKNOWN && !CommonUtils.isEmpty(type.getName()) && Character.isLetter(type.getName().charAt(0))) {
                    typeNames.add(type.getName());
                }
            }
            return typeNames.toArray(new String[typeNames.size()]);
        }
    }

}
