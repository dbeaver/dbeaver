package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

public class YashanDBTestUtils {
    static YashanDBTableColumn addColumn(YashanDBTableBase table, String columnName, String columnType, int ordinalPosition) throws DBException {
        YashanDBTableColumn column = new YashanDBTableColumn(table);
        column.setName(columnName);
        column.setTypeName(columnType);
        column.setOrdinalPosition(ordinalPosition);
        List<YashanDBTableColumn> cachedAttributes = (List<YashanDBTableColumn>) table.getCachedAttributes();
        cachedAttributes.add(column);
        return column;
    }

    static DBEObjectMaker getManagerForClass(Class<?> objectClass) {
        return DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(objectClass, DBEObjectMaker.class);
    }

}
