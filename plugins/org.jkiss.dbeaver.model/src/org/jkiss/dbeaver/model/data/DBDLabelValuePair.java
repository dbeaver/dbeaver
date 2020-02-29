/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.data;

/**
 * Label value pair
 */
public class DBDLabelValuePair implements Comparable {

    private final String label;
    private Object value;

    public DBDLabelValuePair(String label, Object value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value + " (" + label + ")";
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof DBDLabelValuePair) {
            final DBDLabelValuePair lvp = (DBDLabelValuePair) o;
            if (value == lvp.value) {
                return 0;
            }
            if (value == null) {
                return -1;
            }
            if (lvp.value == null) {
                return 1;
            }
            if (value instanceof Comparable && value.getClass() == lvp.value.getClass()) {
                return ((Comparable) value).compareTo(lvp.value);
            }
        }
        return 0;
    }
}
