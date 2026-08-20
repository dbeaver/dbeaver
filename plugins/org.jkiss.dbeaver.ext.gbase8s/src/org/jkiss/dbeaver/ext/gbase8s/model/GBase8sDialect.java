/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.gbase8s.model;

import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

public class GBase8sDialect extends GenericSQLDialect {
    private static final String JSON_TYPE_NAME = "json";

    /**
     * override this method to fix #41838
     * {@inheritDoc}
     *
     * @param attribute   value attribute to help decide whether value should be escaped or not
     * @param value       original value
     * @param strValue    string representation (default result)
     * @return escaped value
     */
    @Override
    public String escapeScriptValue(DBSTypedObject attribute, Object value, String strValue) {
        if (JSON_TYPE_NAME.equalsIgnoreCase(attribute.getTypeName())) {
            return '\'' + escapeString(strValue) + '\'';
        }
        return super.escapeScriptValue(attribute, value, strValue);
    }
}
