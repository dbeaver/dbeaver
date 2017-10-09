/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        value = FunctionNumeric.getComparable(value);
        if (value != null) {
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
