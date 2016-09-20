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

import java.util.ArrayList;
import java.util.List;

/**
 * Mode
 */
public class FunctionMode implements IAggregateFunction {

    private List<Object> cache = new ArrayList<>();

    @Override
    public boolean accumulate(Object value) {
        if (value != null) {
            cache.add(value);
            return true;
        }
        return false;
    }

    @Override
    public Object getResult(int valueCount) {
        Object maxValue = null;
        int maxCount = 0;

        for (int i = 0; i < cache.size(); ++i) {
            int count = 0;
            for (int j = 0; j < cache.size(); ++j) {
                if (cache.get(j).equals(cache.get(i))) {
                    count++;
                }
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = cache.get(i);
            }
        }
//        if (maxCount <= 1) {
//            return null;
//        }
        return maxValue;
    }
}
