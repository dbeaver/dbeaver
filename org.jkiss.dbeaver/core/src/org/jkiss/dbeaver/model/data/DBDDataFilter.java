/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data filter
 */
public class DBDDataFilter {

    private List<DBDColumnOrder> orderColumns = new ArrayList<DBDColumnOrder>();

    public Collection<DBDColumnOrder> getOrderColumns()
    {
        return orderColumns;
    }

    public DBDColumnOrder getOrderColumn(DBCColumnMetaData column)
    {
        for (DBDColumnOrder co : orderColumns) {
            if (co.getColumnMetaData() == column) {
                return co;
            }
        }
        return null;
    }

    public void addOrderColumn(DBDColumnOrder columnOrder)
    {
        orderColumns.add(columnOrder);
    }

    public boolean removeOrderColumn(DBDColumnOrder columnOrder)
    {
        return orderColumns.remove(columnOrder);
    }

    public Object getCriteria()
    {
        return null;
    }

}
