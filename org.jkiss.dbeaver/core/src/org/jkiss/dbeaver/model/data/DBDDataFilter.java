/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data filter
 */
public class DBDDataFilter {

    private List<DBDColumnOrder> orderColumns = new ArrayList<DBDColumnOrder>();
    private List<DBDColumnFilter> filters = new ArrayList<DBDColumnFilter>();
    private String order;
    private String where;

    public DBDDataFilter()
    {
    }

    public DBDDataFilter(DBDDataFilter source)
    {
        for (DBDColumnOrder column : source.orderColumns) {
            orderColumns.add(new DBDColumnOrder(column));
        }
        for (DBDColumnFilter column : source.filters) {
            filters.add(new DBDColumnFilter(column));
        }
        this.order = source.order;
        this.where = source.where;
    }

    public Collection<DBDColumnOrder> getOrderColumns()
    {
        return orderColumns;
    }

    public DBDColumnOrder getOrderColumn(String columnName)
    {
        for (DBDColumnOrder co : orderColumns) {
            if (co.getColumnName().equals(columnName)) {
                return co;
            }
        }
        return null;
    }

    public int getOrderColumnIndex(String columnName)
    {
        for (int i = 0, orderColumnsSize = orderColumns.size(); i < orderColumnsSize; i++) {
            DBDColumnOrder co = orderColumns.get(i);
            if (co.getColumnName().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public void addOrderColumn(DBDColumnOrder columnOrder)
    {
        orderColumns.add(columnOrder);
    }

    public boolean removeOrderColumn(DBDColumnOrder columnOrder)
    {
        return orderColumns.remove(columnOrder);
    }

    public List<DBDColumnFilter> getFilters()
    {
        return filters;
    }

    public DBDColumnFilter getFilterColumn(String columnName)
    {
        for (DBDColumnFilter co : filters) {
            if (co.getColumnName().equals(columnName)) {
                return co;
            }
        }
        return null;
    }

    public void addFilterColumn(DBDColumnFilter columnFilter)
    {
        filters.add(columnFilter);
    }

    public boolean removeFilterColumn(DBDColumnFilter columnFilter)
    {
        return filters.remove(columnFilter);
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public String getWhere()
    {
        return where;
    }

    public void setWhere(String where)
    {
        this.where = where;
    }
}
