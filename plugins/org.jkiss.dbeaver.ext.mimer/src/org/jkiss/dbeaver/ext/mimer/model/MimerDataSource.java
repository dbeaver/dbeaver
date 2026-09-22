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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author Mimer Information Technology
 */
public class MimerDataSource extends GenericDataSource {

    private final UserCache userCache = new UserCache();
    private final GroupCache groupCache = new GroupCache();
    private final ProgramCache programCache = new ProgramCache();
    private final DatabankCache databankCache = new DatabankCache();
    private final ShadowCache shadowCache = new ShadowCache();
    private final LibraryCache libraryCache = new LibraryCache();
    private final SqlFeaturesCache sqlFeaturesCache = new SqlFeaturesCache();
    private final SqlSizingCache sqlSizingCache = new SqlSizingCache();
    private final SqlLanguagesCache sqlLanguagesCache = new SqlLanguagesCache();

    public MimerDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container,
        @NotNull GenericMetaModel metaModel
    ) throws DBException {
        super(monitor, container, metaModel, new MimerSQLDialect());
        initDefaultBinaryPresentation(container);
    }

    /**
     * Registers {@link MimerExecutionContext} instead of the plain {@code
     * GenericExecutionContext} - see that class for why "Execute -&gt; Set Active Schema" needs
     * a Mimer-specific fallback.
     */
    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) throws DBCException {
        return new MimerExecutionContext(instance, type);
    }

    /**
     * Defaults new Mimer SQL connections to Hex for binary value display instead of DBeaver's
     * default String (which renders binary bytes as garbled text). Guarded by {@code
     * !contains(...)} so it only sets once per connection and never clobbers a later user
     * change.
     */
    private static void initDefaultBinaryPresentation(@NotNull DBPDataSourceContainer container) {
        DBPPreferenceStore prefStore = container.getPreferenceStore();
        if (!prefStore.contains(ModelPreferences.RESULT_SET_BINARY_PRESENTATION)) {
            prefStore.setValue(ModelPreferences.RESULT_SET_BINARY_PRESENTATION, DBConstants.BINARY_FORMATS[1].getId());
        }
    }

    /**
     * Forces the right {@link DBPDataKind} by name/code for type names the driver doesn't map
     * correctly through the normal {@code java.sql.Types} lookup. {@code INTERVAL} has no {@code
     * java.sql.Types} constant at all - forced to {@code DATETIME} by prefix, an unavoidable
     * vendor-extension gap rather than a driver bug. {@code NATIONAL CHARACTER} and {@code
     * NATIONAL CHAR LARGE OBJECT} are workaround for a bug in older JDBC drivers.
     * The {@code BUILTIN.GIS_*} names and the other two LOB types ({@code CHARACTER LARGE
     * OBJECT}/{@code BINARY LARGE OBJECT}) hit a similar gap against an older driver version,
     * but we keep them anyway since it's harmless and protects anyone still on
     * an older driver.
     */
    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        String upperTypeName = typeName.toUpperCase(Locale.ENGLISH);
        if (MimerConstants.TYPE_NAMES_UNKNOWN_CATALOG_KIND.contains(upperTypeName)) {
            return DBPDataKind.BINARY;
        }
        if (MimerConstants.TYPE_NAMES_LARGE_OBJECT.contains(upperTypeName)
            || MimerConstants.TYPE_NAMES_LARGE_OBJECT_ABBREVIATED.contains(upperTypeName)) {
            // Only NATIONAL CHAR LARGE OBJECT actually needs this: confirmed live it reports
            // DATA_TYPE -10, not a real java.sql.Types code (NCLOB is 2011) - a driver bug, same
            // "national variant reports a bogus code" shape as NATIONAL CHARACTER below.
            // CHARACTER LARGE OBJECT/BINARY LARGE OBJECT already report the correct CLOB/BLOB
            // codes on their own; forcing them here too is a no-op, kept for simplicity.
            return DBPDataKind.CONTENT;
        }
        if (upperTypeName.startsWith("INTERVAL")) {
            return DBPDataKind.DATETIME;
        }
        if (valueType == Types.ROWID && MimerConstants.isCharacterType(upperTypeName)) {
            // Temporary driver workaround: mimjdbc reports NATIONAL CHARACTER as ROWID (-8)
            // instead of NCHAR (-15) - reported upstream, remove once fixed. Gated on the actual
            // bad code (not just the name) so a user's own character-ish-named domain/type is
            // never affected.
            return DBPDataKind.STRING;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    /**
     * A typed-in precision like {@code "INTERVAL HOUR(2)"} never exactly matches the
     * placeholder-shaped entries {@link MimerDataTypeCache} registers (e.g. {@code "INTERVAL
     * HOUR(p)"}). That's harmless on its own - the DDL still uses the typed text verbatim
     * regardless, see {@code SQLTableColumnManager.DataTypeModifier} - but it means the type
     * never resolves as "known", which logs a spurious debug line and skips {@link
     * #resolveDataKind}'s {@code INTERVAL} handling in favor of the generic path.
     * <p>
     * On an exact-match miss for an {@code INTERVAL} name, normalize any real digits in parens
     * back to the {@code p}/{@code p,s} placeholder shape and retry. That resolves cleanly for
     * any precision/scale the user actually types, not just the literal placeholder text.
     */
    @Nullable
    @Override
    public DBSDataType getLocalDataType(@Nullable String typeName) {
        DBSDataType dataType = super.getLocalDataType(typeName);
        if (dataType == null && typeName != null && typeName.toUpperCase(Locale.ENGLISH).startsWith("INTERVAL")) {
            String normalized = typeName
                .replaceAll("\\(\\s*\\d+\\s*,\\s*\\d+\\s*\\)", "(p,s)")
                .replaceAll("\\(\\s*\\d+\\s*\\)", "(p)");
            if (!normalized.equals(typeName)) {
                dataType = super.getLocalDataType(normalized);
            }
        }
        return dataType;
    }

    // --- Server version gates -------------------------------------------------------------
    // Named per-feature (via isServerVersionAtLeast, inherited from JDBCDataSource) so call
    // sites read as "why" rather than a bare version comparison, and so this is the one place
    // enumerating every version-dependent behavior.

    /**
     * Multi-file databanks ({@code ALTER DATABANK ... ADD FILE}/{@code DROP FILE}) were
     * introduced in Mimer SQL 11.0 - 10.1 only supports a single file per databank. Gates
     * {@link org.jkiss.dbeaver.ext.mimer.edit.MimerDatabankFileManager}'s create/delete.
     */
    public boolean supportsMultiFileDatabanks() {
        return isServerVersionAtLeast(11, 0);
    }

    /**
     * The current {@code CREATE SEQUENCE ... AS <type> START WITH .. INCREMENT BY ..
     * [NO] MINVALUE/MAXVALUE .. [NO] CYCLE} syntax is 11.0+; 10.1 only had {@code CREATE
     * UNIQUE SEQUENCE} (no {@code INCREMENT BY}, no {@code CYCLE}, no databank placement -
     * see {@link MimerSequence#buildCreateDDL}).
     */
    public boolean supportsModernSequenceSyntax() {
        return isServerVersionAtLeast(11, 0);
    }

    /**
     * {@code NEXT VALUE FOR <sequence>} is the current SQL-standard form (11.0+) for reading
     * a sequence's next value in a column default; 10.1 uses the older {@code NEXT_VALUE OF
     * "<schema>"."<seq>"} - see {@code MimerTableColumnManager#prepareAutoIncrementColumn}.
     */
    public boolean supportsNextValueForSyntax() {
        return isServerVersionAtLeast(11, 0);
    }

    /**
     * Clustered indexes ({@code CREATE [UNIQUE] CLUSTERED INDEX ...}) and per-index {@code
     * IGNORE NULLS} were introduced in Mimer SQL 11.1 - no earlier version has the concept at
     * all. See {@link MimerTableIndex} for why this gate matters: the driver's own {@code
     * getIndexInfo()} reports every index as clustered regardless of actual server version.
     */
    public boolean supportsClusteredIndexes() {
        return isServerVersionAtLeast(11, 1);
    }

    /**
     * {@code CREATE INDEX ... INCLUDE (col, ...)} - a Mimer SQL 11.1+ feature letting an index
     * carry extra, non-key columns for an "index lookup only" plan. {@code EXT_ACCESS_PATHS.
     * IS_INCLUDED} (see {@link MimerAccessPathColumn}) is itself an 11.1+-only column even
     * though the view predates it, so this gates {@link MimerTable#loadAccessPaths} from
     * querying it on older servers. INCLUDE columns aren't surfaced distinctly in the UI yet -
     * this is only the column-availability gate.
     */
    public boolean supportsIndexInclude() {
        return isServerVersionAtLeast(11, 1);
    }

    /**
     * External libraries ({@code CREATE LIBRARY ... FILE '...' LANGUAGE CLR}) and CLR-backed
     * procedures/functions ({@code CREATE PROCEDURE/FUNCTION ... LANGUAGE CLR EXTERNAL NAME
     * '...' IN library}) are a Mimer SQL 11.1+ feature.
     * <p>
     * Only .NET/CLR routines exist today, but the {@code LANGUAGE} clause itself is a free-form
     * keyword the server accepts - a future language backend would just need a wider choice in
     * the create dialog, not a new predicate here.
     */
    public boolean supportsExternalLibraries() {
        return isServerVersionAtLeast(11, 1);
    }

    /**
     * {@code SET SCHEMA <name>} - Mimer SQL 11.0+ only; 10.1 has no equivalent
     * statement at all. Gates {@link MimerExecutionContext}'s fallback for "Execute -&gt; Set
     * Active Schema" when the plain JDBC {@code Connection#setSchema()} call fails.
     */
    public boolean supportsSetSchemaStatement() {
        return isServerVersionAtLeast(11, 0);
    }

    /**
     * Every collation across every schema, quoted schema-qualified name pairs flattened into one
     * list - used only by create-dialog pickers that need to offer any collation as a source
     * (e.g. a Domain's {@code COLLATE} clause, a new collation's own {@code FROM} clause), not
     * backed by a tree node of its own. Collations are schema objects in Mimer SQL (see {@link
     * MimerCollation}) - there is no datasource-wide "Collations" folder any more, only each
     * schema's own {@link MimerSchema#getCollations}, which this simply aggregates.
     */
    @NotNull
    public Collection<MimerCollation> getCollations(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<MimerCollation> all = new ArrayList<>();
        for (GenericSchema schema : CommonUtils.safeCollection(getSchemas())) {
            if (schema instanceof MimerSchema mimerSchema) {
                all.addAll(mimerSchema.getCollations(monitor));
            }
        }
        return all;
    }

    @Association
    public Collection<MimerUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDataSource, MimerUser> getUserCache() {
        return userCache;
    }

    @Association
    public Collection<MimerGroup> getGroups(DBRProgressMonitor monitor) throws DBException {
        return groupCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDataSource, MimerGroup> getGroupCache() {
        return groupCache;
    }

    @Association
    public Collection<MimerProgram> getPrograms(DBRProgressMonitor monitor) throws DBException {
        return programCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDataSource, MimerProgram> getProgramCache() {
        return programCache;
    }

    @Association
    public Collection<MimerDatabank> getDatabanks(DBRProgressMonitor monitor) throws DBException {
        return databankCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDataSource, MimerDatabank> getDatabankCache() {
        return databankCache;
    }

    @Association
    public Collection<MimerShadow> getShadows(DBRProgressMonitor monitor) throws DBException {
        return shadowCache.getAllObjects(monitor, this);
    }

    /**
     * {@code INFORMATION_SCHEMA.EXT_LIBRARIES} - external (CLR/.NET today) libraries a
     * procedure/function's body can be implemented in, see {@link MimerLibrary}. Mimer SQL
     * 11.1+ only.
     */
    @Association
    public Collection<MimerLibrary> getLibraries(DBRProgressMonitor monitor) throws DBException {
        return libraryCache.getAllObjects(monitor, this);
    }

    public DBSObjectCache<MimerDataSource, MimerLibrary> getLibraryCache() {
        return libraryCache;
    }

    /**
     * {@code INFORMATION_SCHEMA.SQL_FEATURES} - which SQL:1999/2003 standard features this server
     * supports, one of the three "SQL Standards" reference views (see {@link
     * MimerSqlStandardRow}). All rows, supported or not - the {@code IS_SUPPORTED} column is
     * shown (as a boolean) so the distinction is visible. The parallel SQL:1992 conformance views
     * under {@code FIPS_DOCUMENTATION} don't exist on this server ({@code
     * FIPS_DOCUMENTATION.SQL_FEATURES} throws "table ... not found"), so only the SQL:1999/2003
     * set is modeled here.
     */
    @Association
    public Collection<MimerSqlStandardRow> getSqlFeatures(DBRProgressMonitor monitor) throws DBException {
        return sqlFeaturesCache.getAllObjects(monitor, this);
    }

    /**
     * {@code INFORMATION_SCHEMA.SQL_SIZING} - implementation-defined size limits.
     */
    @Association
    public Collection<MimerSqlStandardRow> getSqlSizing(DBRProgressMonitor monitor) throws DBException {
        return sqlSizingCache.getAllObjects(monitor, this);
    }

    /**
     * {@code INFORMATION_SCHEMA.SQL_LANGUAGES} - supported host language bindings.
     */
    @Association
    public Collection<MimerSqlStandardRow> getSqlLanguages(DBRProgressMonitor monitor) throws DBException {
        return sqlLanguagesCache.getAllObjects(monitor, this);
    }

    /**
     * "Server Info &gt; Server" - the connected Mimer SQL server's own reported name/version,
     * from JDBC metadata DBeaver already cached at connect time ({@link #getInfo()}). No {@code
     * SYSTEM.*} access; Mimer SQL exposes no host-OS / platform value through JDBC metadata.
     */
    @Association
    public Collection<MimerInfoRow> getServerInfoRows(DBRProgressMonitor monitor) {
        List<MimerInfoRow> rows = new ArrayList<>(3);
        rows.add(new MimerInfoRow(this, "Product name", getInfo().getDatabaseProductName()));
        rows.add(new MimerInfoRow(this, "Product version", getInfo().getDatabaseProductVersion()));
        org.osgi.framework.Version v = getInfo().getDatabaseVersion();
        if (v != null) {
            String num = v.getMicro() > 0
                ? v.getMajor() + "." + v.getMinor() + "." + v.getMicro()
                : v.getMajor() + "." + v.getMinor();
            rows.add(new MimerInfoRow(this, "Version", num));
        }
        return rows;
    }

    /**
     * "Server Info &gt; Driver" - the loaded JDBC driver's name/version/class plus where its jar
     * comes from. The JDBC-spec-version row is the only one that opens a short metadata session
     * (DBeaver doesn't cache it); it's simply omitted if the pre-JDBC-4.1 driver doesn't
     * implement {@code getJDBCMajorVersion()}.
     */
    @Association
    public Collection<MimerInfoRow> getDriverInfoRows(DBRProgressMonitor monitor) {
        List<MimerInfoRow> rows = new ArrayList<>(5);
        rows.add(new MimerInfoRow(this, "Driver name", getInfo().getDriverName()));
        rows.add(new MimerInfoRow(this, "Driver version", getInfo().getDriverVersion()));
        rows.add(new MimerInfoRow(this, "Driver class", getContainer().getDriver().getDriverClassName()));
        var libraries = getContainer().getDriver().getDriverLibraries();
        if (!libraries.isEmpty()) {
            rows.add(new MimerInfoRow(this, "Driver library", libraries.get(0).getDisplayName()));
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Mimer SQL JDBC version")) {
            var md = session.getMetaData();
            rows.add(new MimerInfoRow(this, "JDBC version", md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion()));
        } catch (Exception e) {
            // pre-JDBC-4.1 driver may not implement getJDBCMajorVersion() - just skip the row
        }
        return rows;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        userCache.clearCache();
        groupCache.clearCache();
        programCache.clearCache();
        databankCache.clearCache();
        shadowCache.clearCache();
        libraryCache.clearCache();
        sqlFeaturesCache.clearCache();
        sqlSizingCache.clearCache();
        sqlLanguagesCache.clearCache();
        return this;
    }

    static class UserCache extends JDBCObjectCache<MimerDataSource, MimerUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            // DISTINCT, and no IDENT_LOGIN in the SELECT list - EXT_IDENTS has one row per
            // (IDENT_NAME, IDENT_LOGIN) pair, so a user with 2+ OS-user authorizations would
            // otherwise show up as duplicate rows here (see MimerUserAuthorization, which owns
            // reading IDENT_LOGIN per-user instead).
            return session.prepareStatement(
                "SELECT DISTINCT IDENT_NAME, IDENT_TYPE, IDENT_CREATOR, IDENT_SCHEMA\n" +
                "FROM INFORMATION_SCHEMA.EXT_IDENTS\n" +
                "WHERE IDENT_TYPE IN ('USER', 'OS_USER')\n" +
                "ORDER BY IDENT_NAME");
        }

        @Override
        protected MimerUser fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerUser(dataSource, resultSet);
        }
    }

    static class ProgramCache extends JDBCObjectCache<MimerDataSource, MimerProgram> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement(
                "SELECT IDENT_NAME, IDENT_CREATOR, HAS_PASSWORD\n" +
                "FROM INFORMATION_SCHEMA.EXT_IDENTS\n" +
                "WHERE IDENT_TYPE = 'PROGRAM'\n" +
                "ORDER BY IDENT_NAME");
        }

        @Override
        protected MimerProgram fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerProgram(dataSource, resultSet);
        }
    }

    static class GroupCache extends JDBCObjectCache<MimerDataSource, MimerGroup> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            // PUBLIC's presence in EXT_IDENTS varies by server, so it's excluded here and
            // added explicitly below to avoid showing it twice.
            return session.prepareStatement(
                "SELECT IDENT_NAME, IDENT_CREATOR, IDENT_SCHEMA\n" +
                "FROM INFORMATION_SCHEMA.EXT_IDENTS\n" +
                "WHERE IDENT_TYPE = 'GROUP' AND IDENT_NAME <> 'PUBLIC'\n" +
                "UNION ALL\n" +
                "SELECT 'PUBLIC', 'SYSTEM', 'NO' FROM INFORMATION_SCHEMA.EXT_ONEROW\n" +
                "ORDER BY IDENT_NAME");
        }

        @Override
        protected MimerGroup fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerGroup(dataSource, resultSet);
        }
    }

    static class DatabankCache extends JDBCObjectCache<MimerDataSource, MimerDatabank> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            // A multi-file databank has one row per file; DISTINCT collapses them.
            // DATABANK_TYPE = 'PART' rows are the individual files of such databanks - the
            // remaining (non-PART) row is file #1, whose FILE_NAME/MINSIZE/GOALSIZE/MAXSIZE
            // are what ALTER DATABANK's FILE/MINSIZE/GOALSIZE/MAXSIZE clauses actually target
            // (only unambiguous while there's a single file - see MimerDatabank#isSingleFile).
            return session.prepareStatement(
                "SELECT DISTINCT D.DATABANK_NAME, D.DATABANK_CREATOR, D.DATABANK_TYPE, D.IS_ONLINE,\n" +
                "                D.FILE_NAME, D.MINSIZE, D.GOALSIZE, D.MAXSIZE, D.IS_REMOVABLE,\n" +
                "                (SELECT COUNT(*) FROM INFORMATION_SCHEMA.EXT_DATABANKS F\n" +
                "                 WHERE F.DATABANK_NAME = D.DATABANK_NAME) AS FILE_COUNT\n" +
                "FROM INFORMATION_SCHEMA.EXT_DATABANKS D\n" +
                "WHERE D.DATABANK_TYPE <> 'PART'\n" +
                "ORDER BY D.DATABANK_NAME");
        }

        @Override
        protected MimerDatabank fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerDatabank(dataSource, resultSet);
        }
    }

    static class ShadowCache extends JDBCObjectCache<MimerDataSource, MimerShadow> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement(
                "SELECT SHADOW_NAME, SHADOW_CREATOR, DATABANK_NAME, FILE_NAME, IS_ONLINE\n" +
                "FROM INFORMATION_SCHEMA.EXT_SHADOWS\n" +
                "ORDER BY SHADOW_NAME");
        }

        @Override
        protected MimerShadow fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerShadow(dataSource, resultSet);
        }
    }

    static class LibraryCache extends JDBCObjectCache<MimerDataSource, MimerLibrary> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement(
                "SELECT LIBRARY_NAME, LIBRARY_CREATOR, LIBRARY_LANGUAGE, LIBRARY_FILENAME\n" +
                "FROM INFORMATION_SCHEMA.EXT_LIBRARIES\n" +
                "ORDER BY LIBRARY_NAME");
        }

        @Override
        protected MimerLibrary fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerLibrary(dataSource, resultSet);
        }
    }

    static class SqlFeaturesCache extends JDBCObjectCache<MimerDataSource, MimerSqlStandardRow> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SQL_FEATURES");
        }

        @Override
        protected MimerSqlStandardRow fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSqlStandardRow(dataSource, resultSet);
        }
    }

    static class SqlSizingCache extends JDBCObjectCache<MimerDataSource, MimerSqlStandardRow> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SQL_SIZING");
        }

        @Override
        protected MimerSqlStandardRow fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSqlStandardRow(dataSource, resultSet);
        }
    }

    static class SqlLanguagesCache extends JDBCObjectCache<MimerDataSource, MimerSqlStandardRow> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.SQL_LANGUAGES");
        }

        @Override
        protected MimerSqlStandardRow fetchObject(@NotNull JDBCSession session, @NotNull MimerDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSqlStandardRow(dataSource, resultSet);
        }
    }
}
