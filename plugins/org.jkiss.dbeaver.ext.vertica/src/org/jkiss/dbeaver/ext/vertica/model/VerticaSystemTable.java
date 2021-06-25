/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;

import java.util.Date;

public class VerticaSystemTable extends VerticaTable {

    private boolean isSuperUserOnly;
    private boolean isMonitorable;
    private boolean isAccessibleDuringLockdown;

    public VerticaSystemTable(VerticaSchema container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
        if (dbResult != null) {
            this.isSuperUserOnly = JDBCUtils.safeGetBoolean(dbResult, "is_superuser_only");
            this.isMonitorable = JDBCUtils.safeGetBoolean(dbResult, "is_monitorable");
            this.isAccessibleDuringLockdown = JDBCUtils.safeGetBoolean(dbResult, "is_accessible_during_lockdown");
        }
    }

    @Property(viewable = true, order = 5)
    public boolean isSuperUserOnly() {
        return isSuperUserOnly;
    }

    @Property(viewable = true, order = 6)
    public boolean isMonitorable() {
        return isMonitorable;
    }

    @Property(viewable = true, order = 7)
    public boolean isAccessibleDuringLockdown() {
        return isAccessibleDuringLockdown;
    }

    // Hide properties
    @Override
    public String getPartitionExpression() {
        return super.getPartitionExpression();
    }

    // Hide properties
    @Override
    public Date getCreateTime() {
        return super.getCreateTime();
    }

    // Hide properties
    @Override
    public boolean isTempTable() {
        return super.isTempTable();
    }

    // Hide properties
    @Override
    public boolean isHasAggregateProjection() {
        return super.isHasAggregateProjection();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = false, updatable = false, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return super.getDescription();
    }

    @Override
    public boolean isSystem() {
        return true;
    }
}
