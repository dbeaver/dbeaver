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

package org.jkiss.dbeaver;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.registry.formatter.DataFormatterProfile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

import java.util.Arrays;
import java.util.Locale;

/**
 * Preferences constants
 */
public final class ModelPreferences
{
    public enum SeparateConnectionBehavior {
        DEFAULT("Default"),
        ALWAYS("Always"),
        NEVER("Never");
        
        private final String title;
        
        SeparateConnectionBehavior(String title) {
            this.title = title;
        }
        
        public String getTitle() {
            return title;
        }
        
        /**
         * Convert value to SeparateConnectionBehavior option
         */
        public static SeparateConnectionBehavior parse(String value) {
            if ("true".equalsIgnoreCase(value)) {
                return DEFAULT;
            } else if ("false".equalsIgnoreCase(value)) {
                return NEVER;
            } else {
                return CommonUtils.valueOf(SeparateConnectionBehavior.class, value, DEFAULT);
            }
        }
    }
    

    public enum SQLScriptStatementDelimiterMode {
        BLANK_LINE_AND_SEPARATOR(true, false, "Always"),
        ONLY_SEPARATOR(false, false, "Never"),
        SMART(true, true, "Smart");

        public final boolean useBlankLine;
        public final boolean useSmart;

        public final String title;

        SQLScriptStatementDelimiterMode(boolean useBlankLine, boolean useSmart, String title) {
            this.useBlankLine = useBlankLine;
            this.useSmart = useSmart;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public String getName() {
            return this.toString();
        }

        public static SQLScriptStatementDelimiterMode valueByName(String name) {
            if (name == null) {
                return SMART;
            }  else {
                switch (name) {
                    case "true" -> {
                        return SQLScriptStatementDelimiterMode.BLANK_LINE_AND_SEPARATOR;
                    }
                    case "false" -> {
                        return SQLScriptStatementDelimiterMode.ONLY_SEPARATOR;
                    }
                    default -> {
                        try {
                            return SQLScriptStatementDelimiterMode.valueOf(name);
                        } catch (IllegalArgumentException e) {
                            return SQLScriptStatementDelimiterMode.SMART;
                        }
                    }
                }
            }
        }

        @NotNull
        public static SQLScriptStatementDelimiterMode fromPreferences(@NotNull DBPPreferenceStore preferenceStore) {
            return valueByName(preferenceStore.getString(ModelPreferences.SCRIPT_STATEMENT_DELIMITER_BLANK));
        }
    }

    public enum IPType {
        IPV4("IPv4"),
        IPV6("IPv6"),
        AUTO("Auto");

        private final String title;

        IPType(@NotNull String title) {
            this.title = title;
        }

        @NotNull
        public static IPType getPreferredStack() {
            return CommonUtils.valueOf(
                IPType.class,
                preferences.getString(PROP_PREFERRED_IP_STACK),
                AUTO
            );
        }

        @NotNull
        public static IPType getPreferredAddresses() {
            return CommonUtils.valueOf(
                IPType.class,
                preferences.getString(PROP_PREFERRED_IP_ADDRESSES),
                AUTO
            );
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.model";
    public static final String CLIENT_TIMEZONE = "java.client.timezone";
    public static final String CLIENT_BROWSER = "swt.client.browser";

    public static final String PROP_USE_WIN_TRUST_STORE_TYPE = "connections.useWinTrustStoreType"; //$NON-NLS-1$
    public static final String PROP_PREFERRED_IP_STACK = "connections.preferredIPType"; //$NON-NLS-1$
    public static final String PROP_PREFERRED_IP_ADDRESSES = "connections.preferredIPAddresses"; //$NON-NLS-1$

    public static final String NOTIFICATIONS_ENABLED = "notifications.enabled"; //$NON-NLS-1$
    public static final String NOTIFICATIONS_CLOSE_DELAY_TIMEOUT = "notifications.closeDelay"; //$NON-NLS-1$
    public static final String NOTIFICATIONS_SOUND_ENABLED = "notifications.soundEnabled"; //$NON-NLS-1$
    public static final String NOTIFICATIONS_SOUND_VOLUME = "notifications.soundVolume"; //$NON-NLS-1$

    public static final String DICTIONARY_MAX_ROWS = "dictionary.max.rows";

    public static final String QUERY_ROLLBACK_ON_ERROR = "query.rollback-on-error"; //$NON-NLS-1$

    public static final String EXECUTE_RECOVER_ENABLED = "execute.recover.enabled"; //$NON-NLS-1$
    public static final String EXECUTE_RECOVER_RETRY_COUNT = "execute.recover.retryCount"; //$NON-NLS-1$
    public static final String EXECUTE_CANCEL_CHECK_TIMEOUT = "execute.cancel.checkTimeout"; //$NON-NLS-1$

    public static final String DEFAULT_CONNECTION_NAME_PATTERN = "navigator.settings.default.connectionPattern";
    public static final String CONNECTION_OPEN_TIMEOUT = "connection.open.timeout"; //$NON-NLS-1$
    public static final String CONNECTION_VALIDATION_TIMEOUT = "connection.validation.timeout"; //$NON-NLS-1$
    public static final String CONNECTION_CLOSE_ON_SLEEP = "connection.closeOnSleep"; //$NON-NLS-1$
    public static final String CONNECTION_CLOSE_TIMEOUT = "connection.close.timeout"; //$NON-NLS-1$

    public static final String SCRIPT_STATEMENT_DELIMITER = "script.sql.delimiter"; //$NON-NLS-1$
    public static final String SCRIPT_IGNORE_NATIVE_DELIMITER = "script.sql.ignoreNativeDelimiter"; //$NON-NLS-1$
    public static final String SCRIPT_STATEMENT_DELIMITER_BLANK = "script.sql.delimiter.blank"; //$NON-NLS-1$
    public static final String QUERY_REMOVE_TRAILING_DELIMITER = "script.sql.query.remove.trailing.delimiter"; //$NON-NLS-1$

    public static final String MEMORY_CONTENT_MAX_SIZE = "content.memory.maxsize"; //$NON-NLS-1$
    public static final String CONTENT_HEX_ENCODING = "content.hex.encoding"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_CLOB = "content.cache.clob"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_BLOB = "content.cache.blob"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_MAX_SIZE = "content.cache.maxsize"; //$NON-NLS-1$
    public static final String META_SEPARATE_CONNECTION = "database.meta.separate.connection"; //$NON-NLS-1$
    public static final String META_DISABLE_EXTRA_READ = "database.meta.disableAdditionalRead"; //$NON-NLS-1$
    public static final String META_CASE_SENSITIVE = "database.meta.casesensitive"; //$NON-NLS-1$
    public static final String META_USE_SERVER_SIDE_FILTERS = "database.meta.server.side.filters"; //$NON-NLS-1$
    public static final String META_EXTRA_DDL_INFO = "database.meta.extra.ddl.info"; //$NON-NLS-1$

    public static final String META_CLIENT_NAME_DISABLE = "database.meta.client.name.disable"; //$NON-NLS-1$
    public static final String META_CLIENT_NAME_OVERRIDE = "database.meta.client.name.override"; //$NON-NLS-1$
    public static final String META_CLIENT_NAME_VALUE = "database.meta.client.name.value"; //$NON-NLS-1$

    public static final String CONNECT_USE_ENV_VARS = "database.connect.processEnvVars"; //$NON-NLS-1$

    public static final String RESULT_NATIVE_DATETIME_FORMAT = "resultset.format.datetime.native"; //$NON-NLS-1$
    public static final String RESULT_NATIVE_NUMERIC_FORMAT = "resultset.format.numeric.native"; //$NON-NLS-1$
    public static final String RESULT_SCIENTIFIC_NUMERIC_FORMAT = "resultset.format.numeric.scientific"; //$NON-NLS-1$
    public static final String RESULT_TRANSFORM_COMPLEX_TYPES = "resultset.transform.complex.type"; //$NON-NLS-1$

    public static final String RESULT_REFERENCE_DESCRIPTION_COLUMN_PATTERNS = "resultset.reference.value.description.column.patterns"; //$NON-NLS-1$

    // Network
    public static final String NET_TUNNEL_PORT_MIN = "net.tunnel.port.min"; //$NON-NLS-1$
    public static final String NET_TUNNEL_PORT_MAX = "net.tunnel.port.max"; //$NON-NLS-1$

    // ResultSet
    public static final String RESULT_SET_USE_FETCH_SIZE = "resultset.fetch.size"; //$NON-NLS-1$
    public static final String RESULT_SET_MAX_ROWS_USE_SQL = "resultset.maxrows.sql"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_PRESENTATION = "resultset.binary.representation"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_STRING_MAX_LEN = "resultset.binary.stringMaxLength"; //$NON-NLS-1$
    // This will ignore label in result set metadata and will use names always (some buggy drivers return description or other crap in labels - #1952)
    public static final String RESULT_SET_IGNORE_COLUMN_LABEL = "resultset.column.label.ignore"; //$NON-NLS-1$

    public static final String RESULT_SET_REREAD_ON_SCROLLING = "resultset.reread.on.scroll"; //$NON-NLS-1$
    public static final String RESULT_SET_MAX_ROWS = "resultset.maxrows"; //$NON-NLS-1$


    public static final String SQL_PARAMETERS_ENABLED = "sql.parameter.enabled"; //$NON-NLS-1$
    public static final String SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED = "sql.parameter.ddl.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_ENABLED = "sql.parameter.anonymous.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_MARK = "sql.parameter.mark"; //$NON-NLS-1$
    public static final String SQL_NAMED_PARAMETERS_PREFIX = "sql.parameter.prefix"; //$NON-NLS-1$
    public static final String SQL_CONTROL_COMMAND_PREFIX = "sql.command.prefix"; //$NON-NLS-1$
    public static final String SQL_VARIABLES_ENABLED = "sql.variables.enabled"; //$NON-NLS-1$
    public static final String SQL_FILTER_FORCE_SUBSELECT = "sql.query.filter.force.subselect"; //$NON-NLS-1$

    public final static String SQL_FORMAT_KEYWORD_CASE = "sql.format.keywordCase";
    public final static String SQL_FORMAT_EXTERNAL_CMD = "sql.format.external.cmd";
    public final static String SQL_FORMAT_EXTERNAL_FILE = "sql.format.external.file";
    //public final static String SQL_FORMAT_EXTERNAL_DIR = "sql.format.external.dir";
    public final static String SQL_FORMAT_EXTERNAL_TIMEOUT = "sql.format.external.timeout";
    public final static String SQL_FORMAT_LF_BEFORE_COMMA = "sql.format.lf.before.comma";
    public static final String SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET = "sql.format.break.before.close.bracket";
    public static final String SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES = "sql.format.insert.delimiters.in.empty_lines";

    public static final String READ_EXPENSIVE_PROPERTIES = "database.props.expensive"; //$NON-NLS-1$
    public static final String READ_EXPENSIVE_STATISTICS = "database.stats.expensive"; //$NON-NLS-1$
    
    // Driver and proxy settings. They have prefix UI_ by historical reasons.
    public static final String UI_DRIVERS_VERSION_UPDATE = "ui.drivers.version.update"; //$NON-NLS-1$
    public static final String UI_DRIVERS_HOME = "ui.drivers.home"; //$NON-NLS-1$
    public static final String UI_PROXY_HOST = "ui.proxy.host"; //$NON-NLS-1$
    public static final String UI_PROXY_PORT = "ui.proxy.port"; //$NON-NLS-1$
    public static final String UI_PROXY_USER = "ui.proxy.user"; //$NON-NLS-1$
    public static final String UI_PROXY_PASSWORD = "ui.proxy.password"; //$NON-NLS-1$
    public static final String UI_DRIVERS_SOURCES = "ui.drivers.sources"; //$NON-NLS-1$
    public static final String UI_DRIVERS_GLOBAL_LIBRARIES = "ui.drivers.global.libraries"; //$NON-NLS-1$
    public static final String UI_MAVEN_REPOSITORIES = "ui.maven.repositories"; //$NON-NLS-1$

    public static final String NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS = "navigator.show.folder.placeholders"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_ALPHABETICALLY = "navigator.sort.case.insensitive"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_FOLDERS_FIRST = "navigator.sort.forlers.first"; //$NON-NLS-1$

    public static final String PLATFORM_LANGUAGE = "platform.language"; //$NON-NLS-1$

    public static final String TRANSACTIONS_SMART_COMMIT = "transaction.smart.commit"; //$NON-NLS-1$
    public static final String TRANSACTIONS_SMART_COMMIT_RECOVER = "transaction.smart.commit.recover"; //$NON-NLS-1$
    public static final String TRANSACTIONS_SHOW_NOTIFICATIONS = "transaction.show.notifications"; //$NON-NLS-1$
    public static final String TRANSACTIONS_AUTO_CLOSE_ENABLED = "transaction.auto.close.enabled"; //$NON-NLS-1$
    public static final String TRANSACTIONS_AUTO_CLOSE_TTL = "transaction.auto.close.ttl"; //$NON-NLS-1$

    public static final String DICTIONARY_COLUMN_DIVIDER = "resultset.dictionary.columnDivider"; //$NON-NLS-1$
    public static final String RESULT_SET_USE_DATETIME_EDITOR = "resultset.datetime.editor";

    private static Bundle mainBundle;
    private static DBPPreferenceStore preferences;

    public static void setMainBundle(Bundle mainBundle) {
        ModelPreferences.mainBundle = mainBundle;
        ModelPreferences.preferences = new BundlePreferenceStore(mainBundle);
        initializeDefaultPreferences(ModelPreferences.preferences);
    }

    public static Bundle getMainBundle() {
        return mainBundle;
    }

    public static DBPPreferenceStore getPreferences() {
        return preferences;
    }

    private static void initializeDefaultPreferences(DBPPreferenceStore store) {
        // Notifications
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT, 3000L);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_SOUND_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_SOUND_VOLUME, 100);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.DICTIONARY_MAX_ROWS, 200);
        // Common
        PrefUtils.setDefaultPreferenceValue(store, QUERY_ROLLBACK_ON_ERROR, false);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_RECOVER_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_RECOVER_RETRY_COUNT, 1);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_CANCEL_CHECK_TIMEOUT, 0);
        PrefUtils.setDefaultPreferenceValue(store, DEFAULT_CONNECTION_NAME_PATTERN, GeneralUtils.variablePattern(DBPConnectionConfiguration.VAR_HOST_OR_DATABASE));
        PrefUtils.setDefaultPreferenceValue(store, CLIENT_TIMEZONE, DBConstants.DEFAULT_TIMEZONE);
        PrefUtils.setDefaultPreferenceValue(store, CLIENT_BROWSER, "");
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_OPEN_TIMEOUT, 0);
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_VALIDATION_TIMEOUT, 10000);
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_CLOSE_ON_SLEEP, RuntimeUtils.isMacOS());
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_CLOSE_TIMEOUT, 5000);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER, SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_IGNORE_NATIVE_DELIMITER, false);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER_BLANK, SQLScriptStatementDelimiterMode.BLANK_LINE_AND_SEPARATOR);
        PrefUtils.setDefaultPreferenceValue(store, QUERY_REMOVE_TRAILING_DELIMITER, true);

        PrefUtils.setDefaultPreferenceValue(store, MEMORY_CONTENT_MAX_SIZE, 10000);
        PrefUtils.setDefaultPreferenceValue(store, META_SEPARATE_CONNECTION, SeparateConnectionBehavior.DEFAULT.name());
        PrefUtils.setDefaultPreferenceValue(store, META_CASE_SENSITIVE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_DISABLE_EXTRA_READ, false);
        PrefUtils.setDefaultPreferenceValue(store, META_EXTRA_DDL_INFO, true);
        PrefUtils.setDefaultPreferenceValue(store, META_USE_SERVER_SIDE_FILTERS, true);

        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_DISABLE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_OVERRIDE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_VALUE, "");

        PrefUtils.setDefaultPreferenceValue(store, CONNECT_USE_ENV_VARS, true);

        PrefUtils.setDefaultPreferenceValue(store, RESULT_NATIVE_DATETIME_FORMAT, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_NATIVE_NUMERIC_FORMAT, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SCIENTIFIC_NUMERIC_FORMAT, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_TRANSFORM_COMPLEX_TYPES, true);

        PrefUtils.setDefaultPreferenceValue(store, RESULT_REFERENCE_DESCRIPTION_COLUMN_PATTERNS, String.join("|", DBVEntity.DEFAULT_DESCRIPTION_COLUMN_PATTERNS));

        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_REREAD_ON_SCROLLING, true);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_MAX_ROWS, 200);

        PrefUtils.setDefaultPreferenceValue(store, CONTENT_HEX_ENCODING, GeneralUtils.getDefaultFileEncoding());
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_CLOB, true);
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_BLOB, false);
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_MAX_SIZE, 1000000);

        // Network
        PrefUtils.setDefaultPreferenceValue(store, NET_TUNNEL_PORT_MIN, 10000);
        PrefUtils.setDefaultPreferenceValue(store, NET_TUNNEL_PORT_MAX, 60000);

        // ResultSet
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_MAX_ROWS_USE_SQL, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_BINARY_PRESENTATION, DBConstants.BINARY_FORMATS[0].getId());
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_BINARY_STRING_MAX_LEN, 32);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_USE_FETCH_SIZE, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_IGNORE_COLUMN_LABEL, false);

        // QM
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
            QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES, DBCExecutionPurpose.USER + "," + DBCExecutionPurpose.USER_FILTERED + "," + DBCExecutionPurpose.USER_SCRIPT);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);

        // SQL
        PrefUtils.setDefaultPreferenceValue(store, SQL_PARAMETERS_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, SQL_PARAMETERS_IN_EMBEDDED_CODE_ENABLED, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_ANONYMOUS_PARAMETERS_ENABLED, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_ANONYMOUS_PARAMETERS_MARK, String.valueOf(SQLConstants.DEFAULT_PARAMETER_MARK));
        PrefUtils.setDefaultPreferenceValue(store, SQL_NAMED_PARAMETERS_PREFIX, String.valueOf(SQLConstants.DEFAULT_PARAMETER_PREFIX));
        PrefUtils.setDefaultPreferenceValue(store, SQL_CONTROL_COMMAND_PREFIX, String.valueOf(SQLConstants.DEFAULT_CONTROL_COMMAND_PREFIX));
        PrefUtils.setDefaultPreferenceValue(store, SQL_VARIABLES_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FILTER_FORCE_SUBSELECT, false);

        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_KEYWORD_CASE, "");
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_LF_BEFORE_COMMA, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_CMD, "");
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_TIMEOUT, 2000);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_BREAK_BEFORE_CLOSE_BRACKET, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_INSERT_DELIMITERS_IN_EMPTY_LINES, false);

        PrefUtils.setDefaultPreferenceValue(store, READ_EXPENSIVE_PROPERTIES, false);
        PrefUtils.setDefaultPreferenceValue(store, READ_EXPENSIVE_STATISTICS, false);

        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_HOST, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_PORT, 1080);
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_USER, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_PASSWORD, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_VERSION_UPDATE, false);
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_HOME, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_SOURCES, "https://dbeaver.io/files/jdbc/");

        PrefUtils.setDefaultPreferenceValue(store, PROP_USE_WIN_TRUST_STORE_TYPE, RuntimeUtils.isWindows());

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SMART_COMMIT, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_AUTO_CLOSE_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_AUTO_CLOSE_TTL, 30 * 60);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS, true);

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.DICTIONARY_COLUMN_DIVIDER, " ");
        // Data formats
        DataFormatterProfile.initDefaultPreferences(store, Locale.getDefault());

        // Network expert settings
        PrefUtils.setDefaultPreferenceValue(store, PROP_PREFERRED_IP_STACK, IPType.AUTO.name());
        PrefUtils.setDefaultPreferenceValue(store, PROP_PREFERRED_IP_ADDRESSES, IPType.AUTO.name());
    }
}
