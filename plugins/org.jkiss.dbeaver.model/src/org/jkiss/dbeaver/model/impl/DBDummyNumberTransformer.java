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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Shows numeric value if it is not zero and not MAX_VALUE
 */
public class DBDummyNumberTransformer implements IPropertyValueTransformer<DBSObject, Number> {


    @Override
    public Number transform(DBSObject object, Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double && (value.doubleValue() == 0.0 || value.doubleValue() == Double.MAX_VALUE)) {
            return null;
        } else if (value instanceof Float && (value.floatValue() == 0.0 || value.floatValue() == Float.MAX_VALUE)) {
            return null;
        } else if ((value.longValue() == 0 || value.longValue() == Long.MAX_VALUE || value.longValue() == Integer.MAX_VALUE)) {
            return null;
        }

        return value;
    }
}
