/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreExtension
 */
public class PostgreExtension implements PostgreObject, PostgreScriptObject {

    private static final Log log = Log.getLog(PostgreExtension.class);

    private PostgreSchema schema;
    private long oid;
    private String name;
    private String version;
    private Map<Long, String> tableConditions;

    public PostgreExtension(PostgreSchema schema, ResultSet dbResult)
        throws SQLException
    {
        this.schema = schema;
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        this.name = JDBCUtils.safeGetString(dbResult, "extname");
        this.version = JDBCUtils.safeGetString(dbResult, "extversion");
        try {
            Long[] extTableIDs = JDBCUtils.safeGetArray(dbResult, "extconfig");
            String[] extTableConditions = JDBCUtils.safeGetArray(dbResult, "extcondition");
            if (extTableIDs != null && extTableConditions != null) {
                if (extTableIDs.length != extTableConditions.length) {
                    log.error("extconfig.length <> extcondition.length");
                } else {
                    tableConditions = new LinkedHashMap<>();
                    for (int i = 0; i < extTableIDs.length; i++) {
                        tableConditions.put(extTableIDs[i], extTableConditions[i]);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public PostgreSchema getSchema() {
        return schema;
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Property(viewable = true, order = 4)
    public String getVersion() {
        return version;
    }

    public boolean isExtensionTable(PostgreTableBase table) {
        return tableConditions != null && tableConditions.containsKey(table.getObjectId());
    }

    public String getExternalTableCondition(long tableOid) {
        return tableConditions  == null ? null : tableConditions.get(tableOid);
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return schema;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return schema.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return schema.getDatabase();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return
            "-- Extension: " + getName() + "\n\n" +
            "-- DROP EXTENSION " + getName() + ";\n\n" +
            "CREATE EXTENSION " + getName() + "\n\t" +
            "SCHEMA " + DBUtils.getQuotedIdentifier(getSchema()) + "\n\t" +
            "VERSION " + version;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {

    }
}
