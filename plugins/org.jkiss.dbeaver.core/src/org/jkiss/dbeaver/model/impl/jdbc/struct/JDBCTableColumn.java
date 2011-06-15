/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.util.Set;
import java.util.TreeSet;

/**
 * JDBC abstract table column
 */
public abstract class JDBCTableColumn<TABLE_TYPE extends JDBCTable> extends JDBCColumn implements DBPSaveableObject {

    private final TABLE_TYPE table;
    private boolean persisted;

    protected JDBCTableColumn(TABLE_TYPE table, boolean persisted)
    {
        this.table = table;
        this.persisted = persisted;
    }

    protected JDBCTableColumn(TABLE_TYPE table, boolean persisted, String name, String typeName, int valueType, int ordinalPosition, long maxLength, int scale, int radix, int precision, boolean nullable)
    {
        super(name, typeName, valueType, ordinalPosition, maxLength, scale, radix, precision, nullable);
        this.table = table;
        this.persisted = persisted;
    }

    public TABLE_TYPE getTable()
    {
        return table;
    }

    @Property(name = "Column Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 10)
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
    }

    public static class ColumnTypeNameListProvider implements IPropertyValueListProvider<JDBCTableColumn> {

        public boolean allowCustomValue()
        {
            return true;
        }

        public Object[] getPossibleValues(JDBCTableColumn column)
        {
            Set<String> typeNames = new TreeSet<String>();
            if (column.getDataSource() instanceof DBPDataTypeProvider) {
                for (DBSDataType type : ((DBPDataTypeProvider) column.getDataSource()).getDataTypes()) {
                    if (type.getDataKind() != DBSDataKind.UNKNOWN && !CommonUtils.isEmpty(type.getName()) && Character.isLetter(type.getName().charAt(0))) {
                        typeNames.add(type.getName());
                    }
                }
            }
            return typeNames.toArray(new String[typeNames.size()]);
        }
    }

}
