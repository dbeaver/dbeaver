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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraint;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableReal;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class GreenplumUtils {

    @Nullable
    static int[] readDistributedColumns(@NotNull DBRProgressMonitor monitor, @NotNull PostgreTableReal table) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Greenplum table distributed columns")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                if (((GreenplumDataSource) table.getDataSource()).isGreenplumVersionAtLeast(session.getProgressMonitor(), 6, 0)) {
                    try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT distkey FROM pg_catalog.gp_distribution_policy WHERE localoid=" + table.getObjectId())) {
                        if (dbResult.next()) {
                            return PostgreUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, 1));
                        } else {
                            return null;
                        }
                    }
                } else {
                    try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT attrnums FROM pg_catalog.gp_distribution_policy WHERE localoid=" + table.getObjectId())) {
                        if (dbResult.next()) {
                            return PostgreUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, 1));
                        } else {
                            return null;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    static List<PostgreTableColumn> getDistributionTableColumns(@NotNull DBRProgressMonitor monitor, List<PostgreTableColumn> distributionColumns, @NotNull PostgreTableReal table) throws DBException {
        // Get primary key
        PostgreTableConstraint pk = null;
        Collection<PostgreTableConstraint> tableConstraints = CommonUtils.safeCollection(table.getConstraints(monitor));
        // First - search PK in table
        Optional<PostgreTableConstraint> constraint = tableConstraints.stream().filter(key -> key.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY).findFirst();
        if (constraint.isPresent()) {
            pk = constraint.get();
        } else {
            // If no PK in the table - then search first UK for distribution
            Optional<PostgreTableConstraint> tableConstraint = tableConstraints.stream().filter(key -> key.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY).findFirst();
            if (tableConstraint.isPresent()) {
                pk = tableConstraint.get();
            }
        }
        if (pk != null) {
            List<DBSEntityAttribute> pkAttrs = DBUtils.getEntityAttributes(monitor, pk);
            if (!CommonUtils.isEmpty(pkAttrs)) {
                distributionColumns = new ArrayList<>(pkAttrs.size());
                for (DBSEntityAttribute attr : pkAttrs) {
                    distributionColumns.add((PostgreTableColumn) attr);
                }
            }
        }
        return distributionColumns;
    }

    private static boolean isDistributedByReplicated(DBRProgressMonitor monitor, @NotNull PostgreTableReal table) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Greenplum table distributed columns")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT policytype FROM pg_catalog.gp_distribution_policy WHERE localoid=" + table.getObjectId())) {
                    if (dbResult.next()) {
                        return CommonUtils.equalObjects(JDBCUtils.safeGetString(dbResult, 1), "r");
                    } else {
                        return false;
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    private static String getPartitionData(@NotNull DBRProgressMonitor monitor, @NotNull PostgreTableReal table) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read Greenplum table partition data")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT pg_get_partition_def('" + table.getSchema().getName() + "." + table.getName() + "'::regclass, true, false);")) {
                    if (dbResult.next()) {
                        String result = dbResult.getString(1);
                        if (result != null && result.startsWith("PARTITION ")) {
                            return result;
                        }
                        return null;
                    } else {
                        return null;
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    static void addObjectModifiersToDDL(@NotNull DBRProgressMonitor monitor, @NotNull StringBuilder ddl, @NotNull PostgreTableReal table, List<PostgreTableColumn> distributionColumns, boolean supportsReplicatedDistribution) throws DBCException {
        ddl.append("\nDISTRIBUTED ");
        if (supportsReplicatedDistribution && table.isPersisted() && GreenplumUtils.isDistributedByReplicated(monitor, table)) {
            ddl.append("REPLICATED");
        } else if (!CommonUtils.isEmpty(distributionColumns)) {
            ddl.append("BY (");
            for (int i = 0; i < distributionColumns.size(); i++) {
                if (i > 0) ddl.append(", ");
                ddl.append(DBUtils.getQuotedIdentifier(distributionColumns.get(i)));
            }
            ddl.append(")");
        } else {
            ddl.append("RANDOMLY");
        }

        String partitionData = table.isPersisted() ? GreenplumUtils.getPartitionData(monitor, table) : null;
        if (partitionData != null) {
            ddl.append("\n");
            ddl.append(partitionData);
        }
    }
}
