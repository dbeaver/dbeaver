/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *               2017 Andrew Khitrin   (andrew@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.sql.ResultSet;
import java.util.Map;

public class PostgreTablePartition extends PostgreTable implements DBSTablePartition {

    public static final String CAT_PARTITIONING = "Partitioning";

    private String partitionExpression;
    private PostgreTable partitionOf;

    public PostgreTablePartition(PostgreTable container) {
        super(container);
        this.partitionExpression = "FOR VALUES ";
        this.setPartition(true);
        this.setName("newpartition");
        this.partitionOf = container;
    }

    public PostgreTablePartition(PostgreTableContainer container, ResultSet dbResult) {
        super(container, dbResult);
        this.setPartition(true);
        this.partitionExpression = JDBCUtils.safeGetString(dbResult, "partition_expr");
    }

    @NotNull
    @Override
    public DBSTable getParentTable() {
        return partitionOf;
    }

    @Override
    public boolean needFullPath() {
        // Postgres tables can be queried directly without a parent table.
        return false;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 60)
    @Nullable
    public String getPartitionExpression() {
        return partitionExpression;
    }

    public void setPartitionExpression(String expr) {
        this.partitionExpression = expr;
    }


    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        options.put(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS, true);
        options.put(OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, false);
        options.put(OPTION_INCLUDE_NESTED_OBJECTS, false);
        options.put(OPTION_INCLUDE_PERMISSIONS, false);
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    public PostgreTable getPartitionOf() {
        return partitionOf;
    }

    @Override
    public boolean isSubPartition() {
        return partitionOf instanceof PostgreTablePartition;
    }

    @Nullable
    @Override
    public DBSTablePartition getPartitionParent() {
        if (partitionOf instanceof PostgreTablePartition) {
            return (PostgreTablePartition) partitionOf;
        }
        return null;
    }
}
