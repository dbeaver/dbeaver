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

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.query.DBQCondition;
import org.jkiss.dbeaver.model.data.query.DBQOrderColumn;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data filter
 */
public class DBDDataFilter {

    private List<DBQOrderColumn> orderColumns = new ArrayList<DBQOrderColumn>();
    private List<DBQCondition> filters = new ArrayList<DBQCondition>();
    private String order;
    private String where;

    public DBDDataFilter()
    {
    }

    public DBDDataFilter(DBDDataFilter source)
    {
        for (DBQOrderColumn column : source.orderColumns) {
            orderColumns.add(new DBQOrderColumn(column));
        }
        for (DBQCondition column : source.filters) {
            filters.add(new DBQCondition(column));
        }
        this.order = source.order;
        this.where = source.where;
    }

    public Collection<DBQOrderColumn> getOrderColumns()
    {
        return orderColumns;
    }

    public DBQOrderColumn getOrderColumn(String columnName)
    {
        for (DBQOrderColumn co : orderColumns) {
            if (co.getColumnName().equals(columnName)) {
                return co;
            }
        }
        return null;
    }

    public int getOrderColumnIndex(String columnName)
    {
        for (int i = 0, orderColumnsSize = orderColumns.size(); i < orderColumnsSize; i++) {
            DBQOrderColumn co = orderColumns.get(i);
            if (co.getColumnName().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public void addOrderColumn(DBQOrderColumn columnOrder)
    {
        orderColumns.add(columnOrder);
    }

    public boolean removeOrderColumn(DBQOrderColumn columnOrder)
    {
        return orderColumns.remove(columnOrder);
    }

    public void clearOrderColumns()
    {
        orderColumns.clear();
    }

    public List<DBQCondition> getFilters()
    {
        return filters;
    }

    public DBQCondition getFilterColumn(String columnName)
    {
        for (DBQCondition co : filters) {
            if (co.getColumnName().equals(columnName)) {
                return co;
            }
        }
        return null;
    }

    public void addFilterColumn(DBQCondition columnFilter)
    {
        filters.add(columnFilter);
    }

    public boolean removeFilterColumn(DBQCondition columnFilter)
    {
        return filters.remove(columnFilter);
    }

    public void clearFilterColumns()
    {
        filters.clear();
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

    public boolean hasConditions()
    {
        return !CommonUtils.isEmpty(getFilters()) || !CommonUtils.isEmpty(where);
    }

    public void appendConditionString(DBPDataSource dataSource, StringBuilder query)
    {
        appendConditionString(dataSource, null, query);
    }

    public void appendConditionString(DBPDataSource dataSource, String conditionTable, StringBuilder query)
    {
        boolean hasWhere = false;
        if (!CommonUtils.isEmpty(getFilters())) {
            for (DBQCondition filter : getFilters()) {
                if (hasWhere) query.append(" AND "); //$NON-NLS-1$
                hasWhere = true;
                if (conditionTable != null) {
                    query.append(conditionTable).append('.');
                }
                query.append(DBUtils.getQuotedIdentifier(dataSource, filter.getColumnName()));
                final String condition = filter.getCondition();
                final char firstChar = condition.trim().charAt(0);
                if (!Character.isLetter(firstChar) && firstChar != '=' && firstChar != '>' && firstChar != '<' && firstChar != '!') {
                    query.append('=').append(condition);
                } else {
                    query.append(' ').append(condition);
                }
            }
        }
        if (!CommonUtils.isEmpty(getWhere())) {
            if (hasWhere) query.append(" AND "); //$NON-NLS-1$
            query.append(getWhere());
        }
    }

    public int setConditionParameters(DBPDataSource dataSource, int paramIndex)
    {
        return paramIndex;
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
