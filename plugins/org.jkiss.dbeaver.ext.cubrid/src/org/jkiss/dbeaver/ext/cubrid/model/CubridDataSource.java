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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.dpi.DPIContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CubridDataSource extends GenericDataSource
{
    private final CubridMetaModel metaModel;
    private boolean supportMultiSchema;
    private boolean isEOLVersion;
    private final CubridPrivilageCache privilageCache;
    private final CubridServerCache serverCache;
    private ArrayList<CubridCharset> charsets;
    private Map<String, CubridCollation> collations;

    public CubridDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container,
            @NotNull CubridMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new CubridSQLDialect());
        this.metaModel = new CubridMetaModel();
        this.privilageCache = new CubridPrivilageCache();
        this.serverCache = new CubridServerCache();
    }

    @DPIContainer
    @NotNull
    @Override
    public CubridDataSource getDataSource() {
        return this;
    }

    @NotNull
    public List<GenericSchema> getCubridUsers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return this.getSchemas();
    }
    
    @NotNull
    public List<CubridPrivilage> getCubridPrivilages(@NotNull DBRProgressMonitor monitor) throws DBException {
        return privilageCache.getAllObjects(monitor, this);
    }

    public CubridServerCache getServerCache() {
        return serverCache;
    }

    @Nullable
    public List<CubridServer> getCubridServers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return serverCache.getAllObjects(monitor, this);
    }

    @NotNull
    public CubridServer getCubridServer(@NotNull DBRProgressMonitor monitor, @Nullable String name) throws DBException {
        return serverCache.getObject(monitor, this, name);
    }

    @NotNull
    public boolean supportsServer() {
        return getSupportMultiSchema();
    }
    
    @NotNull
    public CubridPrivilageCache getCubridPrivilageCache() {
        return privilageCache;
    }

    @Nullable
    @Override
    public GenericTableBase findTable(
            @NotNull DBRProgressMonitor monitor,
            @Nullable String catalogName,
            @Nullable String schemaName,
            @NotNull String tableName)
            throws DBException {
        if (schemaName != null) {
            return this.getSchema(schemaName).getTable(monitor, tableName);
        } else {
            String[] schemas = tableName.split("\\.");
            if (schemas.length > 1) {
                return this.getSchema(schemas[0].toUpperCase()).getTable(monitor, schemas[1]);
            } else {
                for (GenericSchema schema : this.getCubridUsers(monitor)) {
                    GenericTableBase table = schema.getTable(monitor, tableName);
                    if (table != null) {
                        return table;
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    public CubridMetaModel getMetaModel() {
        return metaModel;
    }

    @NotNull
    public Collection<CubridCharset> getCharsets() {
        return charsets;
    }

    @NotNull
    public CubridCollation getCollation(String name) {
        return collations.get(name);
    }

    @NotNull
    @Override
    public Collection<? extends DBSDataType> getDataTypes(@NotNull DBRProgressMonitor monitor) throws DBException {
        Map<String, DBSDataType> types = new HashMap<>();
        for (DBSDataType dataType : super.getDataTypes(monitor)) {
            types.put(dataType.getName(), dataType);
        }
        return types.values();
    }

    @Nullable
    public CubridCharset getCharset(@NotNull String name) {
        for (CubridCharset charset : charsets) {
            if (charset.getName().equals(name)) {
                return charset;
            }
        }
        return null;
    }

    @NotNull
    public ArrayList<String> getCollations() {
        ArrayList<String> collationList = new ArrayList<String>(collations.keySet());
        return collationList;
    }

    public void loadCharsets(@NotNull DBRProgressMonitor monitor) throws DBException {
        charsets = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load charsets")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("select * from db_charset")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        CubridCharset charset = new CubridCharset(this, dbResult);
                        charsets.add(charset);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Load charsets failed", e);
        }
        charsets.sort(DBUtils.<CubridCharset>nameComparator());
    }

    public void loadCollations(@NotNull DBRProgressMonitor monitor) throws DBException {
        collations = new LinkedHashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load collations")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("show collation")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String charsetName = JDBCUtils.safeGetString(dbResult, "charset");
                        CubridCharset charset = getCharset(charsetName);
                        CubridCollation collation = new CubridCollation(charset, dbResult);
                        collations.put(collation.getName(), collation);
                        charset.addCollation(collation);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Load collations failed", e);
        }
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.initialize(monitor);
        if (!isEOLVersion()) {
            loadCharsets(monitor);
            loadCollations(monitor);
        } else {
            DBWorkbench.getPlatformUI().showMessageBox("Connected CUBRID Info", "The connected CUBRID is an EOL version. Limited features are available.", false);
        }
    }

    @NotNull
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        serverCache.clearCache();
        privilageCache.clearCache();
        return this;
    }

    public boolean getSupportMultiSchema() {
        return this.supportMultiSchema;
    }

    public void setSupportMultiSchema(@NotNull boolean supportMultiSchema) {
        this.supportMultiSchema = supportMultiSchema;
    }

    @NotNull
    public boolean isEOLVersion() {
        return isEOLVersion;
    }

    public void setEOLVersion(@NotNull boolean isEOLVersion) {
        this.isEOLVersion = isEOLVersion;
    }

    public class CubridServerCache extends JDBCObjectCache<CubridDataSource, CubridServer> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull CubridDataSource container)
                throws SQLException {
            String sql = "select * from db_server";
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            return dbStat;
        }

        @Nullable
        @Override
        protected CubridServer fetchObject(
                @NotNull JDBCSession session,
                @NotNull CubridDataSource container,
                @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {
            return new CubridServer(container, dbResult);
        }
    }
    
    public class CubridPrivilageCache extends JDBCObjectCache<CubridDataSource, CubridPrivilage> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull CubridDataSource container)
                throws SQLException {
            String sql = "select * from db_user";
            final JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            return dbStat;
        }

        @Nullable
        @Override
        protected CubridPrivilage fetchObject(
                @NotNull JDBCSession session,
                @NotNull CubridDataSource container,
                @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(dbResult, "name");
            return new CubridPrivilage(container,name, dbResult);
        }
    }

    @NotNull
    @Override
    public boolean splitProceduresAndFunctions() {
        return true;
    }

}
