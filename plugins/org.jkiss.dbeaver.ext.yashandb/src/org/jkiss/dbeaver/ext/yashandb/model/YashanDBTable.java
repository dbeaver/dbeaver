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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OraclePrivTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablespace;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;


/**
 * YashanDBTable
 */
public class YashanDBTable extends OracleTable {

    private static final Log log = Log.getLog(OracleTable.class);

    public YashanDBTable(DBRProgressMonitor monitor, OracleSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
    }

    public YashanDBTable(OracleSchema schema, String name) {
        super(schema, name);
    }

    @Override
    public Collection<OraclePrivTable> getTablePrivs(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support yet
        return Collections.emptyList();
    }

    @Override
    @Property(viewable = true, order = 22, editable = true, updatable = false, listProvider = TablespaceListProvider.class)
    @LazyProperty(cacheValidator = OracleTablespace.TablespaceReferenceValidator.class)
    public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
        // YashanDB not support move table space
        return OracleTablespace.resolveTablespaceReference(monitor, this, null);
    }

    @Override
    protected void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        // YashanDB not support ALL_TAB_STATS_HISTORY yet
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "TABLES")
                    + " WHERE OWNER=? AND TABLE_NAME=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.pctFree = JDBCUtils.safeGetInt(dbResult, "PCT_FREE");
                        additionalInfo.iniTrans = JDBCUtils.safeGetInt(dbResult, "INI_TRANS");
                        additionalInfo.maxTrans = JDBCUtils.safeGetInt(dbResult, "MAX_TRANS");
                        additionalInfo.blocks = JDBCUtils.safeGetInt(dbResult, "BLOCKS");
                        additionalInfo.emptyBlocks = JDBCUtils.safeGetInt(dbResult, "EMPTY_BLOCKS");
                    } else {
                        log.warn("Cannot find table '" + getFullyQualifiedName(DBPEvaluationContext.UI) + "' metadata");
                    }
                    additionalInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }

    }
}
