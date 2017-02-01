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
