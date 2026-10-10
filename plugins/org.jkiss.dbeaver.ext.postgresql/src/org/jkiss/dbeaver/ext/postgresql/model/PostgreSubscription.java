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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.internal.PostgreSQLMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.net.SSLConfigurationMethod;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PostgreSubscription extends PostgreReplicationObject {
    private static final int CONNECTION_TEST_TIMEOUT_SECONDS = 30;
    private static final String CONNECTION_TEST_PREFIX = "dbeaver_connection_test_";
    private static final List<String> SYNCHRONOUS_COMMIT_MODES = List.of("off", "local", "remote_write", "on", "remote_apply");

    private String connectionInfo = "";
    private String publications = "";
    private String slotName;
    private String synchronousCommit = "off";
    private boolean enabled = true;
    private boolean binary;
    private boolean streaming;
    private boolean parallelStreaming;
    private String twoPhaseState;
    private boolean connect = true;
    private boolean createSlot = true;
    private boolean copyData = true;

    public PostgreSubscription(@NotNull PostgreDatabase database, @NotNull String name) {
        super(database, name);
    }

    public PostgreSubscription(@NotNull PostgreDatabase database, @NotNull ResultSet result) {
        super(database, result, "subname", "subowner");
        enabled = JDBCUtils.safeGetBoolean(result, "subenabled");
        binary = JDBCUtils.safeGetBoolean(result, "subbinary");
        // PostgreSQL 16 changed substream from boolean to a character ('f', 't', or 'p').
        String stream = JDBCUtils.safeGetString(result, "substream");
        streaming = "t".equals(stream) || "true".equals(stream) || "p".equals(stream);
        parallelStreaming = "p".equals(stream);
        slotName = JDBCUtils.safeGetString(result, "subslotname");
        synchronousCommit = CommonUtils.notEmpty(JDBCUtils.safeGetString(result, "subsynccommit"));
        twoPhaseState = JDBCUtils.safeGetString(result, "subtwophasestate");
        String[] names = PostgreUtils.safeGetStringArray(result, "subpublications");
        publications = names == null ? "" : String.join("\n", names);
    }

    @NotNull
    @Property(editable = true, updatable = true, password = true, order = 4)
    public String getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(@NotNull String connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @NotNull
    public static String buildConnectionInfo(@NotNull Map<String, String> parameters) {
        // libpq keyword/value strings use backslash escapes, independently of SQL string literal escaping.
        return parameters.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> entry.getKey() + "='" + entry.getValue().replace("\\", "\\\\").replace("'", "\\'") + "'")
            .collect(Collectors.joining(" "));
    }

    public static boolean supportsPublisherConnection(@NotNull DBPDataSourceContainer connection) {
        String authModel = connection.getConnectionConfiguration().getAuthModelId();
        String actualAuthModel = connection.getActualConnectionConfiguration().getAuthModelId();
        return supportsPublicationDiscoveryConnection(connection)
            && (CommonUtils.isEmpty(authModel) || AuthModelDatabaseNative.ID.equals(authModel))
            && (CommonUtils.isEmpty(actualAuthModel) || AuthModelDatabaseNative.ID.equals(actualAuthModel));
    }

    public static boolean supportsPublicationDiscoveryConnection(@NotNull DBPDataSourceContainer connection) {
        return connection.getDriver().getDataSourceProvider() instanceof PostgreDataSourceProvider;
    }

    @NotNull
    public static List<String> readPublisherPublicationNames(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer connection
    ) throws DBException {
        // Use the desktop connection's active database, including URL-based and non-native-auth datasources.
        PostgreDataSource dataSource = getPublicationDiscoveryDataSource(monitor, connection);
        return readPublicationNames(monitor, dataSource.getDefaultInstance());
    }

    @NotNull
    public static List<String> readPublisherPublicationNames(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer connection,
        @NotNull String databaseName
    ) throws DBException {
        PostgreDataSource dataSource = getPublicationDiscoveryDataSource(monitor, connection);
        PostgreDatabase database = dataSource.getDatabase(databaseName);
        if (database == null) {
            throw new DBException("Publisher database is not available in this connection: " + databaseName);
        }
        return readPublicationNames(monitor, database);
    }

    @NotNull
    private static PostgreDataSource getPublicationDiscoveryDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer connection
    ) throws DBException {
        if (monitor.isCanceled()) {
            throw new DBException("Publication request canceled");
        }
        if (!supportsPublicationDiscoveryConnection(connection)) {
            throw new DBException("Select a PostgreSQL datasource to read publications");
        }
        if (!connection.isConnected() && !connection.connect(monitor, true, !connection.isTemporary())) {
            throw new DBException("Cannot connect to the publisher to read publications");
        }
        if (!(connection.getDataSource() instanceof PostgreDataSource dataSource)
            || !dataSource.getServerType().supportsLogicalReplication()) {
            throw new DBException("The publisher must support PostgreSQL logical replication");
        }
        if (monitor.isCanceled()) {
            throw new DBException("Publication request canceled");
        }
        return dataSource;
    }

    @NotNull
    private static List<String> readPublicationNames(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreDatabase database
    ) throws DBException {
        // Read names afresh on a separate desktop connection, without changing cached metadata or user transactions.
        String task = PostgreSQLMessages.action_read_publication_names;
        try (PostgreExecutionContext context = database.openIsolatedContext(monitor, task, null);
             JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.META, task);
             JDBCPreparedStatement statement = session.prepareStatement("SELECT pubname FROM pg_catalog.pg_publication ORDER BY pubname")) {
            statement.setQueryTimeout(CONNECTION_TEST_TIMEOUT_SECONDS);
            List<String> names = new ArrayList<>();
            try (JDBCResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    if (monitor.isCanceled()) {
                        throw new DBException("Publication request canceled");
                    }
                    names.add(result.getString(1));
                }
            }
            return List.copyOf(names);
        } catch (SQLException e) {
            throw new DBException("Cannot read publisher publication names", e);
        }
    }

    @NotNull
    public static String mergePublicationSelection(
        @NotNull String currentNames, @NotNull List<String> availableNames, @NotNull List<String> selectedNames
    ) {
        // Keep manually entered names that are not in the fetched list; only replace the checklist's own selections.
        LinkedHashSet<String> names = currentNames.lines().filter(name -> !name.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        names.removeAll(availableNames);
        names.addAll(selectedNames);
        return String.join("\n", names);
    }

    @NotNull
    public static String formatPublicationNames(@NotNull List<String> names) {
        return names.stream().filter(name -> !name.isBlank()).map(name -> {
            if (name.indexOf(',') >= 0 || name.indexOf('"') >= 0 || !name.equals(name.trim())) {
                return '"' + name.replace("\"", "\"\"") + '"';
            }
            return name;
        }).collect(Collectors.joining(", "));
    }

    @NotNull
    public static List<String> parsePublicationNames(@NotNull String text) throws DBException {
        if (text.isBlank()) {
            return List.of();
        }
        // The single-line field uses commas, so quoted names must preserve commas, whitespace, and doubled quotes.
        List<String> names = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        boolean quotedField = false;
        boolean closedQuote = false;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() && quoted) {
                throw new DBException("Unterminated quoted publication name");
            }
            char ch = i == text.length() ? ',' : text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        value.append(ch);
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    value.append(ch);
                }
            } else if (ch == ',') {
                String name = quotedField ? value.toString() : value.toString().trim();
                if (name.isBlank()) {
                    throw new DBException("Publication names must not be empty");
                }
                names.add(name);
                value.setLength(0);
                quotedField = false;
                closedQuote = false;
            } else if (ch == '"') {
                if (closedQuote || !value.toString().isBlank()) {
                    throw new DBException("Double quotes must enclose the whole publication name");
                }
                value.setLength(0);
                quoted = true;
                quotedField = true;
            } else if (closedQuote) {
                if (!Character.isWhitespace(ch)) {
                    throw new DBException("Expected a comma after the quoted publication name");
                }
            } else {
                value.append(ch);
            }
        }
        return List.copyOf(names);
    }

    @NotNull
    public static DBPConnectionConfiguration copyPublisherConnectionConfiguration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer connection
    ) throws DBException {
        if (!supportsPublisherConnection(connection)) {
            throw new DBException("Select a PostgreSQL connection with native database authentication");
        }
        // Resolve stored credentials without connecting to the database or persisting any settings.
        connection.isCredentialsSaved();
        if (monitor.isCanceled()) {
            throw new DBException("Publisher connection selection canceled");
        }
        if (!supportsPublisherConnection(connection)) {
            throw new DBException("Publisher connection requires native database authentication");
        }
        DBPConnectionConfiguration copy = DBPConnectionConfiguration.copyWithIndependentRuntimeAttributes(
            connection.getConnectionConfiguration());
        DBPConnectionConfiguration actual = connection.getActualConnectionConfiguration();
        if (CommonUtils.isEmpty(copy.getUserName())) {
            copy.setUserName(actual.getUserName());
        }
        if (CommonUtils.isEmpty(copy.getUserPassword())) {
            copy.setUserPassword(actual.getUserPassword());
        }
        // Preserve the saved remote endpoint, not the desktop's resolved SSH tunnel address/port.
        for (DBWHandlerConfiguration handler : connection.getConnectionConfiguration().getHandlers()) {
            if (!PostgreConstants.HANDLER_SSL.equals(handler.getId())) {
                copy.removeHandler(handler.getId());
            }
        }
        if (copy.getConfigurationType() != DBPDriverConfigurationType.MANUAL) {
            throw new DBException("Use a host-based connection, or enter a custom libpq connection string in the subscription dialog");
        }
        buildConnectionInfo(copy);
        return copy;
    }

    @NotNull
    public static String buildConnectionInfo(@NotNull DBPConnectionConfiguration configuration) throws DBException {
        // Replication runs inside the subscriber server, so desktop tunnels and Java authentication/SSL cannot be reused.
        if (configuration.getConfigurationType() != DBPDriverConfigurationType.MANUAL) {
            throw new DBException("Use host-based configuration, or enter a custom libpq connection string in the subscription dialog");
        }
        if (CommonUtils.isNotEmpty(configuration.getAuthModelId())
            && !AuthModelDatabaseNative.ID.equals(configuration.getAuthModelId())) {
            throw new DBException("Publisher configuration requires native database authentication");
        }
        if (CommonUtils.isEmptyTrimmed(configuration.getHostName()) || CommonUtils.isEmptyTrimmed(configuration.getDatabaseName())) {
            throw new DBException("Enter the publisher host and database as reachable from the subscriber server");
        }
        String port = CommonUtils.isEmptyTrimmed(configuration.getHostPort())
            ? Integer.toString(PostgreConstants.DEFAULT_PORT) : configuration.getHostPort().trim();
        try {
            int portNumber = Integer.parseInt(port);
            if (portNumber < 1 || portNumber > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new DBException("Enter a port number between 1 and 65535");
        }
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", configuration.getHostName().trim());
        parameters.put("port", port);
        parameters.put("dbname", configuration.getDatabaseName().trim());
        parameters.put("user", CommonUtils.notEmpty(configuration.getUserName()));
        parameters.put("password", CommonUtils.notEmpty(configuration.getUserPassword()));
        for (String property : new String[]{"sslmode", "sslrootcert", "sslcert", "sslkey", "sslpassword"}) {
            parameters.put(property, CommonUtils.notEmpty(configuration.getProperty(property)));
        }
        if (CommonUtils.toBoolean(configuration.getProperty(PostgreConstants.PROP_SSL)) && parameters.get("sslmode").isEmpty()) {
            parameters.put("sslmode", "verify-full");
        }
        if (CommonUtils.isNotEmpty(configuration.getProperty("sslfactory"))
            || CommonUtils.isNotEmpty(configuration.getProperty("sslpasswordcallback"))) {
            throw new DBException("Java SSL factories and password callbacks cannot be used by the subscriber server");
        }
        for (DBWHandlerConfiguration handler : configuration.getHandlers()) {
            if (!handler.isEnabled()) {
                continue;
            }
            if (!PostgreConstants.HANDLER_SSL.equals(handler.getId())) {
                throw new DBException("Desktop network handlers cannot be used by the subscriber server; configure a directly reachable publisher");
            }
            addPublisherSSLParameters(parameters, handler);
        }
        return buildConnectionInfo(parameters);
    }

    private static void addPublisherSSLParameters(@NotNull Map<String, String> parameters, @NotNull DBWHandlerConfiguration ssl)
        throws DBException {
        if (SSLConfigurationMethod.KEYSTORE.name().equals(ssl.getStringProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD))
            || ssl.getBooleanProperty(SSLHandlerTrustStoreImpl.PROP_SSL_SELF_SIGNED_CERT)
            || ssl.getBooleanProperty(SSLHandlerTrustStoreImpl.PROP_SSL_FORCE_TLS12)
            || CommonUtils.isNotEmpty(ssl.getStringProperty(PostgreConstants.PROP_SSL_FACTORY))) {
            throw new DBException("Use certificate files accessible to the subscriber server, not Java keystores or SSL factories");
        }
        String mode = ssl.getStringProperty(PostgreConstants.PROP_SSL_MODE);
        parameters.put("sslmode", CommonUtils.isEmpty(mode) ? "verify-full" : mode);
        Map<String, String> certificates = Map.of(
            "sslrootcert", SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT,
            "sslcert", SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT,
            "sslkey", SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY);
        for (Map.Entry<String, String> certificate : certificates.entrySet()) {
            if (CommonUtils.isNotEmpty(ssl.getSecureProperty(certificate.getValue() + SSLHandlerTrustStoreImpl.CERT_VALUE_SUFFIX))) {
                throw new DBException("Embedded SSL certificates cannot be used by the subscriber server; specify server-side certificate paths");
            }
            String path = ssl.getStringProperty(certificate.getValue());
            if (CommonUtils.isNotEmpty(path)) {
                parameters.put(certificate.getKey(), path);
            }
        }
        parameters.put("sslpassword", CommonUtils.notEmpty(ssl.getPassword()));
    }

    public static void testPublisherConnection(
        @NotNull DBRProgressMonitor monitor,
        @NotNull PostgreDatabase subscriber,
        @NotNull String publisherConnection
    ) throws DBException {
        if (publisherConnection.isBlank()) {
            throw new DBException("Enter a publisher connection string");
        }
        if (monitor.isCanceled()) {
            throw new DBException("Publisher connection test canceled");
        }
        String task = PostgreSQLMessages.action_test_subscription_connection;
        String probeName = CONNECTION_TEST_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String identifier = DBUtils.getQuotedIdentifier(subscriber.getDataSource(), probeName);
        // A slot-free CREATE SUBSCRIPTION is transactional. An absent publication prevents table synchronization checks.
        // Always roll back on a separate connection, so the test cannot commit user work or leave local/remote resources.
        String sql = "CREATE SUBSCRIPTION " + identifier + " CONNECTION " + SQLUtils.quoteString(subscriber, publisherConnection)
            + " PUBLICATION " + identifier
            + " WITH (connect = true, create_slot = false, enabled = false, copy_data = false, slot_name = NONE)";
        try (PostgreExecutionContext context = subscriber.openIsolatedContext(monitor, task, null);
             JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, task)) {
            // The probe SQL contains credentials; do not include it in DBeaver query history.
            session.enableLogging(false);
            session.setAutoCommit(false);
            try {
                try (JDBCStatement statement = session.createStatement()) {
                    statement.setQueryTimeout(CONNECTION_TEST_TIMEOUT_SECONDS);
                    statement.execute(sql);
                }
            } finally {
                session.rollback();
            }
        } catch (SQLException e) {
            // Driver errors can include the credential-bearing SQL/URI. Return only safe diagnostics, without its cause.
            if (PostgreConstants.EC_PERMISSION_DENIED.equals(e.getSQLState())) {
                throw new DBException("The subscriber user requires permission to create subscriptions and CREATE on this database");
            }
            if (PostgreConstants.EC_QUERY_CANCELED.equals(e.getSQLState())) {
                throw new DBException("Publisher connection test timed out or was canceled");
            }
            throw new DBException("Publisher replication connection test failed (SQL state " + CommonUtils.notEmpty(e.getSQLState())
                + "). Check the server-side address, database, password, and publisher replication privileges");
        }
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, required = true, length = PropertyLength.MULTILINE, order = 5)
    public String getPublications() {
        return publications;
    }

    public void setPublications(@NotNull String publications) {
        this.publications = publications;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 6)
    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(@Nullable String slotName) {
        this.slotName = slotName;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 7)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 8)
    public boolean isBinary() {
        return binary;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 9)
    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
        parallelStreaming = false;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, required = true, order = 10,
        listProvider = SynchronousCommitListProvider.class)
    public String getSynchronousCommit() {
        return synchronousCommit;
    }

    public void setSynchronousCommit(@NotNull String synchronousCommit) {
        this.synchronousCommit = synchronousCommit;
    }

    @NotNull
    public static String[] getSynchronousCommitModes() {
        return SYNCHRONOUS_COMMIT_MODES.toArray(String[]::new);
    }

    public static boolean isValidSynchronousCommit(@NotNull String value) {
        return SYNCHRONOUS_COMMIT_MODES.contains(value);
    }

    public static class SynchronousCommitListProvider implements IPropertyValueListProvider<PostgreSubscription> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @NotNull
        @Override
        public Object[] getPossibleValues(@Nullable PostgreSubscription object) {
            return getSynchronousCommitModes();
        }
    }

    @Nullable
    @Property(viewable = true, optional = true, order = 11)
    public String getTwoPhaseState() {
        return twoPhaseState;
    }

    @Property(editable = true, order = 12, hideExpr = "object.persisted", visibleIf = CreationPropertyValidator.class)
    public boolean isConnect() {
        return connect;
    }

    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    @Property(editable = true, order = 13, hideExpr = "object.persisted", visibleIf = CreationPropertyValidator.class)
    public boolean isCreateSlot() {
        return createSlot;
    }

    public void setCreateSlot(boolean createSlot) {
        this.createSlot = createSlot;
    }

    @Property(editable = true, order = 14, hideExpr = "object.persisted", visibleIf = CreationPropertyValidator.class)
    public boolean isCopyData() {
        return copyData;
    }

    public void setCopyData(boolean copyData) {
        this.copyData = copyData;
    }

    public static class CreationPropertyValidator implements IPropertyValueValidator<PostgreSubscription, Object> {
        @Override
        public boolean isValidValue(@NotNull PostgreSubscription object, @Nullable Object value) {
            return !object.isPersisted();
        }
    }

    @NotNull
    public String getCreateStatement() throws DBException {
        if (getName().isBlank()) {
            throw new DBException("Subscription name cannot be empty");
        }
        if (connectionInfo.isBlank()) {
            throw new DBException("Subscription connection information cannot be empty");
        }
        if (!connect && (enabled || createSlot || copyData)) {
            throw new DBException("A disconnected subscription requires enabled, create_slot, and copy_data to be false");
        }
        return buildDefinition(connectionInfo, connect, createSlot, copyData);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        // Never export publisher credentials or create a remote slot when replaying the metadata DDL.
        return buildDefinition("<publisher connection string>", false, false, false);
    }

    @NotNull
    private String buildDefinition(@NotNull String connection, boolean connect, boolean createSlot, boolean copyData) throws DBException {
        String publicationNames = getPublicationNamesSQL();
        String effectiveSlot = slotName;
        if (!isPersisted() && CommonUtils.isEmpty(effectiveSlot)) {
            effectiveSlot = getName();
        }
        return "CREATE SUBSCRIPTION " + DBUtils.getQuotedIdentifier(this) +
            "\nCONNECTION " + SQLUtils.quoteString(this, connection) +
            "\nPUBLICATION " + publicationNames +
            "\nWITH (connect = " + connect + ", create_slot = " + createSlot + ", copy_data = " + copyData +
            ", enabled = " + (connect && enabled) + ", binary = " + binary +
            ", streaming = " + (parallelStreaming ? "parallel" : Boolean.toString(streaming)) +
            ", synchronous_commit = " + SQLUtils.quoteString(this, synchronousCommit) +
            ", slot_name = " + (effectiveSlot == null ? "NONE" : SQLUtils.quoteString(this, effectiveSlot)) + ");";
    }

    @NotNull
    public String getPublicationNamesSQL() throws DBException {
        String names = publications.lines().filter(name -> !name.isBlank())
            .map(name -> DBUtils.getQuotedIdentifier(getDataSource(), name))
            .collect(Collectors.joining(", "));
        if (names.isEmpty()) {
            throw new DBException("At least one publication is required");
        }
        return names;
    }

    @Override
    public void setPersisted(boolean persisted) {
        if (persisted && !isPersisted() && CommonUtils.isEmpty(slotName)) {
            slotName = getName();
        }
        super.setPersisted(persisted);
        if (persisted) {
            connectionInfo = "";
        }
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getSubscriptionCache().refreshObject(monitor, getDatabase(), this);
    }
}
