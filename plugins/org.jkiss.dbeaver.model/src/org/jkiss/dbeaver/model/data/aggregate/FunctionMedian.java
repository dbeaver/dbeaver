/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.data.aggregate;

import org.jkiss.dbeaver.Log;

import java.util.*;

/**
 * Median
 */
public class FunctionMedian implements IAggregateFunction {

    private static final Log log = Log.getLog(FunctionMedian.class);

    private List<Comparable> cache = new ArrayList<>();

    @Override
    public boolean accumulate(Object value) {
        if (value instanceof Comparable) {
            cache.add((Comparable) value);
            return true;
        }
        return false;
    }

    @Override
    public Object getResult(int valueCount) {
        try {
            Collections.sort(cache);
        } catch (Exception e) {
            log.debug("Can't sort value collection", e);
            return null;
        }

        int size = cache.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return cache.get(middle);
        } else {
            Comparable val1 = cache.get(middle - 1);
            Comparable val2 = cache.get(middle);
            if (val1 instanceof Number && val2 instanceof Number) {
                return (((Number) val1).doubleValue() + ((Number) val2).doubleValue()) / 2.0;
            }
            // Not true median - but we can't evaluate it for non-numeric values
            // So just get first one
            return val1;
        }
    }
}
