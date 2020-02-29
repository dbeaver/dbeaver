/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;

import java.sql.ResultSet;
import java.util.Map;

public class PostgreTablePartition extends PostgreTable {

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
        this.partitionExpression = JDBCUtils.safeGetString(dbResult, "partition_expr");
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
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    public PostgreTable getPartitionOf() {
        return partitionOf;
    }

}
