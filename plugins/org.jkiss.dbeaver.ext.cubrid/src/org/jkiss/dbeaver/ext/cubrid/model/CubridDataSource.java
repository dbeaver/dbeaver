/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class CubridDataSource extends GenericDataSource
{
    private final CubridMetaModel metaModel;
    private final CubridPrivilageCache privilageCache;
    private final CubridServerCache serverCache;
    private boolean supportMultiSchema;
    private boolean supportDbmsOutputPlCsql = false;
    private boolean isEOLVersion;
    private ArrayList<CubridCharset> charsets;
    private Map<String, CubridCollation> collations;
    private boolean isShard = false;
    private String shardType = "SHARD ID";
    private String shardVal = "0";
    private CubridOutputReader outputReader = null;
    private List<String> privilegeGroups;

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

    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new CubridStructureAssistant(this));
        } else if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(outputReader);
        }
        return super.getAdapter(adapter);
    }

    public String getCurrentUser() {
        return getContainer().getConnectionConfiguration().getUserName().toUpperCase();
    }

    public boolean isDBAGroup() {
        return CubridConstants.DBA.equalsIgnoreCase(getCurrentUser())
                || privilegeGroups.contains(CubridConstants.DBA);
    }

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

    public void loadPrivilege(DBRProgressMonitor monitor) throws DBException {
        privilegeGroups = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load privilege Group")) {
            String query = wrapShardQuery("select db_user.name, user_group.name from db_user, table(groups) as groups_tb(user_group) where db_user.name = ?");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                String currentUser = getContainer().getConnectionConfiguration().getUserName().toUpperCase();
                dbStat.setString(1, currentUser);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String groups = JDBCUtils.safeGetString(dbResult, "user_group.name");
                        privilegeGroups.add(groups);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Load privilege failed", e);
        }
    }

    public void loadCharsets(@NotNull DBRProgressMonitor monitor) throws DBException {
        charsets = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Load charsets")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(wrapShardQuery("select * from db_charset"))) {
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
            try (JDBCPreparedStatement dbStat = session.prepareStatement(wrapShardQuery("show collation"))) {
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
            loadPrivilege(monitor);
        } else {
            DBWorkbench.getPlatformUI().showMessageBox(
                "Connected CUBRID Info",
                "The connected CUBRID is an EOL version. Limited features are available.",
                false);
        }
        setTracking(monitor);
    }

    @NotNull
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        serverCache.clearCache();
        privilageCache.clearCache();
        return this;
    }

    @Override
    protected void initializeContextState(DBRProgressMonitor monitor,
            JDBCExecutionContext context,
            JDBCExecutionContext initFrom)
            throws DBException {
        super.initializeContextState(monitor, context, initFrom);

        if (outputReader == null && checkSupportDbmsOutput(monitor, context)) {
            outputReader = new CubridOutputReader();
        }

        if (outputReader != null) {
            outputReader.enableDbmsOutput(monitor, context);
        }
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

    public boolean isShard() {
        return isShard;
    }

    public void setShard(boolean isShard) {
        this.isShard = isShard;
    }

    public void setShardType(String shardType) {
        this.shardType = shardType;
    }

    public void setShardVal(String shardVal) {
        this.shardVal = shardVal;
    }

    public String wrapShardQuery(String sql) {
        if (!isShard || hasShardHint(sql)) {
            return sql;
        }
        return getShardHint() + sql;
    }

    public StringBuilder wrapShardQuery(StringBuilder sql) {
        if (!isShard || hasShardHint(sql.toString())) {
            return sql;
        }
        return sql.insert(0, getShardHint());
    }

    public boolean hasShardHint(String sql) {
        Pattern idPattern = Pattern.compile(CubridConstants.REGEX_PATTERN_SHARD_ID, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Pattern valPattern = Pattern.compile(CubridConstants.REGEX_PATTERN_SHARD_VAL, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        return idPattern.matcher(sql).find() || valPattern.matcher(sql).find();
    }
    private String getShardHint() {
        String hintKey = "SHARD ID".equalsIgnoreCase(shardType) ? "SHARD_ID" : "SHARD_VAL";
        return String.format("/*+ %s(%s) */ ", hintKey, shardVal);
    }

    public CubridShard getCubridShard() {
        return new CubridShard(this, shardType, shardVal);
    }

    public List<CubridShard> getCubridShards() {
        return Collections.singletonList(new CubridShard(this, shardType, shardVal));
    }

    @NotNull
    @Override
    public boolean splitProceduresAndFunctions() {
        return true;
    }

    @Nullable
    @Override
    public GenericSchema getSchema(String name) {
        return super.getSchema(name == null ? null : name.toUpperCase(Locale.ENGLISH));
    }

    private void setTracking(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "set trace")) {
            try (JDBCStatement st = session.createStatement()) {
                if (store.getBoolean(CubridConstants.STATISTIC_TRACE))
                    st.execute(wrapShardQuery("SET TRACE ON"));
                if (!CommonUtils.isEmpty(store.getString(CubridConstants.STATISTIC)))
                    st.execute(wrapShardQuery("set @collect_exec_stats = 1"));
            }
        } catch (SQLException e) {
            throw new DBException("Can't set trace", e);
        }
    }

    public class CubridServerCache extends JDBCObjectCache<CubridDataSource, CubridServer> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull CubridDataSource container)
                throws SQLException {
            String sql = wrapShardQuery("select * from db_server");
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
            String sql = wrapShardQuery("select * from db_user");
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
            return new CubridPrivilage(container, name, dbResult);
        }
    }

    public boolean isSupportEnableDbms() {
        return getContainer().getPreferenceStore().getBoolean(CubridConstants.PREF_DBMS_OUTPUT);
    }

    public boolean isSupportDbmsOutputPlCsql() {
        return supportDbmsOutputPlCsql;
    }

    private boolean checkSupportDbmsOutput(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBCExecutionContext context)
            throws DBException {

        try (JDBCSession session =
                (JDBCSession)
                        context.openSession(
                                monitor, DBCExecutionPurpose.UTIL, "Read Database Version")) {
            readDatabaseServerVersion(session, session.getMetaData());
        } catch (SQLException e) {
            throw new DBException("Check Support DBMSOutput failed", e);
        }

        supportDbmsOutputPlCsql = isServerVersionAtLeast(11, 4);

        return supportDbmsOutputPlCsql;
    }

    private class CubridOutputReader implements DBCServerOutputReader {
        @Override
        public boolean isServerOutputEnabled() {
            return getContainer().getPreferenceStore().getBoolean(CubridConstants.PREF_DBMS_OUTPUT);
        }

        @Override
        public boolean isAsyncOutputReadSupported() {
            return false;
        }

        public void enableDbmsOutput(DBRProgressMonitor monitor, DBCExecutionContext context)
                throws DBCException {
            if (!isServerOutputEnabled()) {
                return;
            }

            int bufferSize =
                    getContainer()
                            .getPreferenceStore()
                            .getInt(CubridConstants.PREF_DBMS_OUTPUT_BUFFER_SIZE);

            try (JDBCSession session =
                    (JDBCSession)
                            context.openSession(
                                    monitor, DBCExecutionPurpose.UTIL, "Enable DBMS output")) {
                CallableStatement cstmt = session.getOriginal().prepareCall("CALL dbms_output.enable(?)");
                cstmt.setInt(1, bufferSize);
                cstmt.execute();
            } catch (SQLException e) {
                throw new DBCException(e, context);
            }
        }

        @Override
        public void readServerOutput(
                @NotNull DBRProgressMonitor monitor,
                @NotNull DBCExecutionContext context,
                @Nullable DBCExecutionResult executionResult,
                @Nullable DBCStatement statement,
                @NotNull DBCOutputWriter output)
                throws DBCException {
            try (JDBCSession session =
                    (JDBCSession)
                            context.openSession(
                                    monitor, DBCExecutionPurpose.UTIL, "Read DBMS output")) {
                try (CallableStatement cstmt =
                        session.getOriginal().prepareCall("CALL dbms_output.get_line(?,?)")) {
                    cstmt.registerOutParameter(1, java.sql.Types.VARCHAR);
                    cstmt.registerOutParameter(2, java.sql.Types.INTEGER);

                    String line;
                    int status = 0;
                    while (status == 0) {
                        cstmt.execute();
                        status = cstmt.getInt(2);
                        if (status == 0) {
                            line = cstmt.getString(1);
                            output.println(null, line);
                        }
                    }

                } catch (SQLException e) {
                    throw new DBCException(e, context);
                }
            }
        }
    }
}
