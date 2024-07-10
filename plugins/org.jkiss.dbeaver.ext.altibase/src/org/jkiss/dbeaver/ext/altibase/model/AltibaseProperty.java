/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class AltibaseProperty implements DBSObject {

    private static final Log log = Log.getLog(AltibaseProperty.class);

    private final AltibaseDataSource dataSource;
    private final String name;
    private String description;
    private boolean dynamic;
    private long attr;

    public AltibaseProperty(AltibaseDataSource dataSource, JDBCResultSet dbResult) {
        final int valueCount;
        final String min;
        final String max;
        String tmp;

        this.dataSource = dataSource;
        name = JDBCUtils.safeGetString(dbResult, "NAME");
        valueCount  = JDBCUtils.safeGetInt(dbResult, "STOREDCOUNT");
        attr = JDBCUtils.safeGetLong(dbResult, "ATTR");
        min  = JDBCUtils.safeGetString(dbResult, "MIN");
        max  = JDBCUtils.safeGetString(dbResult, "MAX");

        // configurable without restarting the server
        dynamic = (attr & AltibaseConstants.IDP_ATTR_RD_READONLY) == AltibaseConstants.IDP_ATTR_RD_WRITABLE;

        // number: current value [min, max]
        if (CommonUtils.isNotEmpty(min) || CommonUtils.isNotEmpty(max)) {
            description = String.format("%s  [%s, %s]",
                    JDBCUtils.safeGetString(dbResult, "VALUE1"),
                    CommonUtils.isEmpty(min) ? "" : min,
                    CommonUtils.isEmpty(max) ? "" : max
                    );
        // string
        } else {
            description = "";
            // Concatenate values: VALUE1, VALUE2, ...
            for (int i = 0; i < valueCount; i++) {
                // Though valueCount is 1, the value could be null.
                tmp = JDBCUtils.safeGetString(dbResult, "VALUE" + (i + 1));
                if (!CommonUtils.isEmpty(tmp)) {
                    if (!CommonUtils.isEmpty(description)) {
                        description += ", ";
                    }
                    description += tmp;
                }
            }
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public boolean getDynamic() {
        return dynamic;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 3)
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return getDataSource();
    }

    @NotNull
    @Override
    public AltibaseDataSource getDataSource() {
        return dataSource;
    }
}
