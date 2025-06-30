/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Local table
 */
public class DBVDataContext extends DBVAbstractContext {

    public static final String VAR_TABLE = "table";
    public static final String VAR_ROW = "row";

    private final Map<String, Object> nsList;
    private final DBSObject dataContainer;
    private final DBDAttributeBinding[] attributes;
    private final Object[] row;
    private final String excludeName;

    public DBVDataContext(
        @NotNull DBSObject dataContainer,
        @NotNull DBDAttributeBinding[] allAttributes,
        @NotNull Object[] row,
        @Nullable String excludeName
    ) {
        this.nsList = DBVUtils.getExpressionNamespaces();
        this.dataContainer = dataContainer;
        this.attributes = allAttributes;
        this.row = row;
        this.excludeName = excludeName;
    }

    @Override
    public Object get(String s) {
        Object ns = nsList.get(s);
        if (ns != null) {
            return ns;
        }
        if (s.equals(excludeName)) {
            return null;
        } else if (s.equals(VAR_ROW)) {
            return new RowInfo();
        } else if (s.equals(VAR_TABLE)) {
            return new TableInfo();
        }
        for (DBDAttributeBinding attr : attributes) {
            if (s.equals(attr.getLabel())) {
                return DBUtils.getAttributeValue(attr, attributes, row);
            }
        }
        return null;
    }

    public class TableInfo extends DBVAbstractContext {
        @Override
        public Object get(String s) {
            return switch (s) {
                case "name" -> dataContainer.getName();
                case "schema", "container" -> dataContainer.getParentObject();
                default -> null;
            };
        }
    }

    public class RowInfo extends DBVAbstractContext {
        @Override
        public Object get(String s) {
            for (DBDAttributeBinding attr : attributes) {
                if (s.equals(attr.getLabel())) {
                    return DBUtils.getAttributeValue(attr, attributes, row);
                }
            }
            return null;
        }
    }
}
