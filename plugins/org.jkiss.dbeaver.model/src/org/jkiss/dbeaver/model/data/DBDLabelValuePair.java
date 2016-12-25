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

package org.jkiss.dbeaver.model.data;

/**
 * Label value pair
 */
public class DBDLabelValuePair implements Comparable {

    private final String label;
    private final Object value;

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
