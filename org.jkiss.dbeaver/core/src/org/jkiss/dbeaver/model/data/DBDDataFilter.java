/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import net.sf.jkiss.utils.CommonUtils;

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

    public void clearOrderColumns()
    {
        orderColumns.clear();
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

    public boolean hasCustomFilters()
    {
        return !filters.isEmpty() || !CommonUtils.isEmpty(this.order) || !CommonUtils.isEmpty(this.where);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DBDDataFilter)) {
            return false;
        }
        DBDDataFilter source = (DBDDataFilter)obj;
        if (orderColumns.size() != source.orderColumns.size() || filters.size() != source.filters.size()) {
            return false;
        }
        for (int i = 0, orderColumnsSize = source.orderColumns.size(); i < orderColumnsSize; i++) {
            if (!orderColumns.get(i).equals(source.orderColumns.get(i))) {
                return false;
            }
        }
        for (int i = 0, filtersSize = source.filters.size(); i < filtersSize; i++) {
            if (!filters.get(i).equals(source.filters.get(i))) {
                return false;
            }
        }
        return CommonUtils.equalObjects(this.order, source.order) &&
            CommonUtils.equalObjects(this.where, source.where);
    }

}
