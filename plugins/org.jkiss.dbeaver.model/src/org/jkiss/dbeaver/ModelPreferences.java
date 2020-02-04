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

package org.jkiss.dbeaver;

import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.osgi.framework.Bundle;

import java.util.Arrays;

/**
 * Preferences constants
 */
public final class ModelPreferences
{
    public static final String PLUGIN_ID = "org.jkiss.dbeaver.model";

    public static final String NOTIFICATIONS_ENABLED = "notifications.enabled"; //$NON-NLS-1$
    public static final String NOTIFICATIONS_CLOSE_DELAY_TIMEOUT = "notifications.closeDelay"; //$NON-NLS-1$

    public static final String QUERY_ROLLBACK_ON_ERROR = "query.rollback-on-error"; //$NON-NLS-1$

    public static final String EXECUTE_RECOVER_ENABLED = "execute.recover.enabled"; //$NON-NLS-1$
    public static final String EXECUTE_RECOVER_RETRY_COUNT = "execute.recover.retryCount"; //$NON-NLS-1$
    public static final String EXECUTE_CANCEL_CHECK_TIMEOUT = "execute.cancel.checkTimeout"; //$NON-NLS-1$

    public static final String CONNECTION_OPEN_TIMEOUT = "connection.open.timeout"; //$NON-NLS-1$
    public static final String CONNECTION_VALIDATION_TIMEOUT = "connection.validation.timeout"; //$NON-NLS-1$
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
    public static final String META_CASE_SENSITIVE = "database.meta.casesensitive"; //$NON-NLS-1$
    public static final String META_USE_SERVER_SIDE_FILTERS = "database.meta.server.side.filters"; //$NON-NLS-1$

    public static final String META_CLIENT_NAME_DISABLE = "database.meta.client.name.disable"; //$NON-NLS-1$
    public static final String META_CLIENT_NAME_OVERRIDE = "database.meta.client.name.override"; //$NON-NLS-1$
    public static final String META_CLIENT_NAME_VALUE = "database.meta.client.name.value"; //$NON-NLS-1$

    public static final String CONNECT_USE_ENV_VARS = "database.connect.processEnvVars"; //$NON-NLS-1$

    public static final String RESULT_NATIVE_DATETIME_FORMAT = "resultset.format.datetime.native"; //$NON-NLS-1$
    public static final String RESULT_TRANSFORM_COMPLEX_TYPES = "resultset.transform.complex.type"; //$NON-NLS-1$

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

    public static final String SQL_PARAMETERS_ENABLED = "sql.parameter.enabled"; //$NON-NLS-1$
    public static final String SQL_PARAMETERS_IN_DDL_ENABLED = "sql.parameter.ddl.enabled"; //$NON-NLS-1$
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

    // Driver and proxy settings. They have prefix UI_ by historical reasons.
    public static final String UI_DRIVERS_VERSION_UPDATE = "ui.drivers.version.update"; //$NON-NLS-1$
    public static final String UI_DRIVERS_HOME = "ui.drivers.home"; //$NON-NLS-1$
    public static final String UI_PROXY_HOST = "ui.proxy.host"; //$NON-NLS-1$
    public static final String UI_PROXY_PORT = "ui.proxy.port"; //$NON-NLS-1$
    public static final String UI_PROXY_USER = "ui.proxy.user"; //$NON-NLS-1$
    public static final String UI_PROXY_PASSWORD = "ui.proxy.password"; //$NON-NLS-1$
    public static final String UI_DRIVERS_SOURCES = "ui.drivers.sources"; //$NON-NLS-1$
    public static final String UI_MAVEN_REPOSITORIES = "ui.maven.repositories"; //$NON-NLS-1$

    public static final String NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS = "navigator.show.folder.placeholders"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_ALPHABETICALLY = "navigator.sort.case.insensitive"; //$NON-NLS-1$
    public static final String NAVIGATOR_SORT_FOLDERS_FIRST = "navigator.sort.forlers.first"; //$NON-NLS-1$

    public static final String PLATFORM_LANGUAGE = "platform.language"; //$NON-NLS-1$
    public static final String TRANSACTIONS_SMART_COMMIT = "transaction.smart.commit"; //$NON-NLS-1$
    public static final String TRANSACTIONS_SMART_COMMIT_RECOVER = "transaction.smart.commit.recover"; //$NON-NLS-1$
    public static final String TRANSACTIONS_SHOW_NOTIFICATIONS = "transaction.show.notifications"; //$NON-NLS-1$

    private static Bundle mainBundle;
    private static DBPPreferenceStore preferences;

    public static synchronized DBPPreferenceStore getPreferences() {
        if (preferences == null) {
            setMainBundle(ModelActivator.getInstance().getBundle());
        }
        return preferences;
    }

    public static void setMainBundle(Bundle mainBundle) {
        ModelPreferences.mainBundle = mainBundle;
        ModelPreferences.preferences = new BundlePreferenceStore(mainBundle);
        initializeDefaultPreferences(ModelPreferences.preferences);
    }

    public static Bundle getMainBundle() {
        return mainBundle;
    }

    private static void initializeDefaultPreferences(DBPPreferenceStore store) {
        // Notifications
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT, 3000L);

        // Common
        PrefUtils.setDefaultPreferenceValue(store, QUERY_ROLLBACK_ON_ERROR, false);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_RECOVER_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_RECOVER_RETRY_COUNT, 1);
        PrefUtils.setDefaultPreferenceValue(store, EXECUTE_CANCEL_CHECK_TIMEOUT, 0);

        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_OPEN_TIMEOUT, 0);
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_VALIDATION_TIMEOUT, 10000);
        PrefUtils.setDefaultPreferenceValue(store, CONNECTION_CLOSE_TIMEOUT, 5000);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER, SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_IGNORE_NATIVE_DELIMITER, false);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER_BLANK, true);
        PrefUtils.setDefaultPreferenceValue(store, QUERY_REMOVE_TRAILING_DELIMITER, true);

        PrefUtils.setDefaultPreferenceValue(store, MEMORY_CONTENT_MAX_SIZE, 10000);
        PrefUtils.setDefaultPreferenceValue(store, META_SEPARATE_CONNECTION, true);
        PrefUtils.setDefaultPreferenceValue(store, META_CASE_SENSITIVE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_USE_SERVER_SIDE_FILTERS, true);

        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_DISABLE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_OVERRIDE, false);
        PrefUtils.setDefaultPreferenceValue(store, META_CLIENT_NAME_VALUE, "");

        PrefUtils.setDefaultPreferenceValue(store, CONNECT_USE_ENV_VARS, true);

        PrefUtils.setDefaultPreferenceValue(store, RESULT_NATIVE_DATETIME_FORMAT, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_TRANSFORM_COMPLEX_TYPES, true);

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
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, GeneralUtils.getMetadataFolder().getAbsolutePath());

        // SQL
        PrefUtils.setDefaultPreferenceValue(store, SQL_PARAMETERS_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, SQL_PARAMETERS_IN_DDL_ENABLED, false);
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

        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_HOST, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_PORT, 1080);
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_USER, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_PROXY_PASSWORD, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_VERSION_UPDATE, false);
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_HOME, "");
        PrefUtils.setDefaultPreferenceValue(store, UI_DRIVERS_SOURCES, "https://dbeaver.io/files/jdbc/");

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SMART_COMMIT, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SMART_COMMIT_RECOVER, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.TRANSACTIONS_SHOW_NOTIFICATIONS, true);
    }
}
