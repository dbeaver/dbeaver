package org.jkiss.dbeaver.ext.db2.edit;

import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.util.ArrayList;
import java.util.List;

public class DB2ColumnDataTypeListProvider implements IPropertyValueListProvider<DB2TableColumn> {

    @Override
    public boolean allowCustomValue()
    {
        return false;
    }

    @Override
    public Object[] getPossibleValues(DB2TableColumn column)
    {
        List<DBSDataType> dataTypes = new ArrayList<DBSDataType>(column.getTable().getDataSource().getDataTypes());
        if (!dataTypes.contains(column.getType())) {
            dataTypes.add(column.getType());
        }
        return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
    }
}