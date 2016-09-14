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

/**
 * FunctionAvg
 */
public class FunctionAvg implements IAggregateFunction {

    double result = Double.NaN;
//    long dateResult = 0;

    @Override
    public boolean accumulate(Object value) {
        if (value instanceof Number) {
            if (Double.isNaN(result)) {
                result = 0.0;
            }
            result += ((Number)value).doubleValue();
            return true;
        }/* else if (value instanceof Date) {
            dateResult += ((Date)value).getTime();
            return true;
        }*/
        return false;
    }

    @Override
    public Object getResult(int valueCount) {
        if (Double.isNaN(result)) {
//            if (dateResult > 0) {
//                return new Date(dateResult / valueCount);
//            }
            return null;
        }
        return result / valueCount;
    }
}
