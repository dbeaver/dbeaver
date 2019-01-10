/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
 * AggregateUtils
 */
public class AggregateUtils {

    public static int compareValues(Comparable val1, Comparable val2) {
        if (val1 instanceof Number && val2 instanceof Number) {
            return (int)(((Number) val1).doubleValue() - ((Number) val2).doubleValue());
        } else if (val1.getClass() == val2.getClass()) {
            return val1.compareTo(val2);
        } else {
            return 0;
        }
    }
}
