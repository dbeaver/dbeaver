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

/**
 * FunctionSum
 */
public class FunctionMax implements IAggregateFunction {

    Comparable result = null;

    @Override
    public boolean accumulate(Object value) {
        if (value instanceof Comparable) {
            if (result == null || AggregateUtils.compareValues((Comparable) value, result) > 0) {
                result = (Comparable) value;
            }
            return true;
        }
        return false;
    }

    @Override
    public Object getResult(int valueCount) {
        return result;
    }

}
