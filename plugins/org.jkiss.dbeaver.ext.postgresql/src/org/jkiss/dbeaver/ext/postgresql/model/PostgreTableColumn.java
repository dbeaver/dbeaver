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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgreTableColumn
 */
public class PostgreTableColumn extends PostgreAttribute<PostgreTableBase> implements PostgrePrivilegeOwner, PostgreScriptObject, GisAttribute, JDBCColumnKeyType {
    private static final Log log = Log.getLog(PostgreTableColumn.class);

    public PostgreTableColumn(DBRProgressMonitor monitor, PostgreTableBase table, PostgreTableColumn source) throws DBException {
        super(monitor, table, source);
    }

    private static class GeometryInfo {
        private String type;
        private int srid = -1;
        public int dimension;
    }

    private GeometryInfo geometryInfo;

    public PostgreTableColumn(PostgreTableBase table) {
        super(table);
    }

    public PostgreTableColumn(DBRProgressMonitor monitor, PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        super(monitor, table, dbResult);
    }

    @Override
    public PostgreSchema getSchema() {
        return getTable().getSchema();
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getTable().getOwner(monitor);
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(monitor, this, getAcl());
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        return null;
    }

    @Override
    public int getAttributeGeometrySRID(DBRProgressMonitor monitor) throws DBCException {
        if (geometryInfo == null) {
            readGeometryInfo(monitor);
        }
        if (geometryInfo != null) {
            return geometryInfo.srid;
        } else {
            return -1;
        }
    }

    @Override
    public String getAttributeGeometryType(DBRProgressMonitor monitor) throws DBCException {
        if (geometryInfo == null) {
            readGeometryInfo(monitor);
        }
        if (geometryInfo != null) {
            return geometryInfo.type;
        } else {
            return null;
        }
    }

    private void readGeometryInfo(DBRProgressMonitor monitor) throws DBCException {
        if (geometryInfo != null) {
            return;
        }

        GeometryInfo gi = new GeometryInfo();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT srid, type,coord_dimension FROM geometry_columns " +
                "WHERE f_table_schema=? AND f_table_name=? AND f_geometry_column=?"))
            {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getTable().getName());
                dbStat.setString(3, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        gi.srid = dbResult.getInt(1);
                        gi.type = dbResult.getString(2);
                        gi.dimension = dbResult.getInt(3);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading geometry info", e);
        }

        geometryInfo = gi;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateObjectDDL(monitor, this, options, false);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }

    @Nullable
    @Override
    protected JDBCColumnKeyType getKeyType() {
        return this;
    }

    @Override
    public boolean isInUniqueKey() {
        final List<PostgreTableConstraintBase> cCache = getTable().getSchema().getConstraintCache().getCachedObjects(getTable());
        if (!CommonUtils.isEmpty(cCache)) {
            for (PostgreTableConstraintBase key : cCache) {
                if (key instanceof PostgreTableConstraint && key.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    List<PostgreTableConstraintColumn> cColumns = ((PostgreTableConstraint) key).getColumns();
                    if (!CommonUtils.isEmpty(cColumns)) {
                        for (PostgreTableConstraintColumn cCol : cColumns) {
                            if (cCol.getAttribute() == this) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isInReferenceKey() {
        return false;
    }

}
