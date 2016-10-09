/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.editors;

import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a list of Exasol Table Column for Exasol Table editors
 * 
 * @author Karl Griesser
 * 
 */
public class ExasolColumnDataTypeListProvider implements IPropertyValueListProvider<ExasolTableColumn> {

    @Override
    public boolean allowCustomValue()
    {
        return false;
    }

    @Override
    public Object[] getPossibleValues(ExasolTableColumn column)
    {
        List<DBSDataType> dataTypes = new ArrayList<DBSDataType>(column.getTable().getDataSource().getLocalDataTypes());
        if (!dataTypes.contains(column.getDataType())) {
            dataTypes.add(column.getDataType());
        }
        return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
    }
}