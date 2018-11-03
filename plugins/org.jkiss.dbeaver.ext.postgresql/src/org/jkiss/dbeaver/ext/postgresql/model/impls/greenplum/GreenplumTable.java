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
package org.jkiss.dbeaver.ext.postgresql.model.impls.greenplum;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraint;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GreenplumTable
 */
public class GreenplumTable extends PostgreTableRegular {

    private static final Log log = Log.getLog(GreenplumTable.class);

    private int[] distributionColumns;

    public GreenplumTable(PostgreSchema catalog) {
        super(catalog);
    }

    public GreenplumTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    @Property(viewable = false, order = 90)
    public List<PostgreTableColumn> getDistributionPolicy(DBRProgressMonitor monitor) throws DBException {
        if (distributionColumns == null) {
            try {
                distributionColumns = readDistributedColumns(monitor);
            } catch (Throwable e) {
                log.error("Error reading distribution policy", e);
            }
            if (distributionColumns == null) {
                distributionColumns = new int[0];
            }
        }

        if (distributionColumns.length == 0) {
            return null;
        }
        List<PostgreTableColumn> columns = new ArrayList<>(distributionColumns.length);
        for (int i = 0; i < distributionColumns.length; i++) {
            PostgreTableColumn attr = getAttributeByPos(monitor, distributionColumns[i]);
            if (attr == null) {
                log.debug("Bad policy attribute position: " + distributionColumns[i]);
            } else {
                columns.add(attr);
            }
        }
        return columns;
    }

    @Nullable
    private int[] readDistributedColumns(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Greenplum table distributed columns")) {
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT attrnums FROM pg_catalog.gp_distribution_policy WHERE localoid=" + getObjectId())) {
                    if (dbResult.next()) {
                        return PostgreUtils.getIntVector(JDBCUtils.safeGetObject(dbResult, 1));
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    @Override
    public void appendTableModifiers(DBRProgressMonitor monitor, StringBuilder ddl) {
        try {
            List<PostgreTableColumn> distributionColumns = getDistributionPolicy(monitor);
            if (CommonUtils.isEmpty(distributionColumns)) {
                // Get primary key
                PostgreTableConstraint pk = null;
                for (PostgreTableConstraint tc : getConstraints(monitor)) {
                    if (tc.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        pk = tc;
                        break;
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
            }

            ddl.append("\nDISTRIBUTED ");
            if (CommonUtils.isEmpty(distributionColumns)) {
                ddl.append("RANDOMLY");
            } else {
                ddl.append("BY (");
                for (int i = 0; i < distributionColumns.size(); i++) {
                    if (i > 0) ddl.append(", ");
                    ddl.append(DBUtils.getQuotedIdentifier(distributionColumns.get(i)));
                }
                ddl.append(")");
            }
        } catch (DBException e) {
            log.error("Error reading Greenplum table properties", e);
        }
    }
}

