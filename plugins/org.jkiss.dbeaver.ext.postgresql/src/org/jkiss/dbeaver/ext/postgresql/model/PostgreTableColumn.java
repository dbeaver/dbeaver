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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.data.type.PostgreGeometryTypeHandler;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgreTableColumn
 */
public class PostgreTableColumn extends PostgreAttribute<PostgreTableBase>
    implements PostgrePrivilegeOwner, PostgreScriptObject, GisAttribute, JDBCColumnKeyType {
    private static final Log log = Log.getLog(PostgreTableColumn.class);

    public PostgreTableColumn(DBRProgressMonitor monitor, PostgreTableBase table, PostgreTableColumn source) throws DBException {
        super(monitor, table, source);
    }

    public PostgreTableColumn(PostgreTableBase table) {
        super(table);
    }

    public PostgreTableColumn(DBRProgressMonitor monitor, PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        super(monitor, table, dbResult);
    }

    @Override
    protected boolean supportsDependencies() {
        return true;
    }

    @NotNull
    @Override
    public PostgreSchema getSchema() {
        return getTable().getSchema();
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getTable().getOwner(monitor);
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(monitor, this, getAcl(), false);
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return null;
    }

    @Override
    public int getAttributeGeometrySRID(DBRProgressMonitor monitor) {
        return PostgreGeometryTypeHandler.getGeometrySRID(getTypeMod());
    }

    @Nullable
    @Override
    public String getAttributeGeometryType(DBRProgressMonitor monitor) {
        return PostgreGeometryTypeHandler.getGeometryType(getTypeMod());
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
        final List<PostgreTableConstraintBase<?>> cCache = getTable().getSchema().getConstraintCache().getCachedObjects(getTable());
        if (!CommonUtils.isEmpty(cCache)) {
            for (PostgreTableConstraintBase<?> key : cCache) {
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
