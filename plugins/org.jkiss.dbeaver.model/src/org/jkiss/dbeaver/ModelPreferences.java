/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.bundle.ModelActivator;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLTokenizedFormatter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.osgi.framework.Bundle;

import java.util.Arrays;

/**
 * Preferences constants
 */
public final class ModelPreferences
{
    public static final String QUERY_ROLLBACK_ON_ERROR = "query.rollback-on-error"; //$NON-NLS-1$

    public static final String SCRIPT_STATEMENT_DELIMITER = "script.sql.delimiter"; //$NON-NLS-1$
    public static final String SCRIPT_IGNORE_NATIVE_DELIMITER = "script.sql.ignoreNativeDelimiter"; //$NON-NLS-1$
    public static final String SCRIPT_STATEMENT_DELIMITER_BLANK = "script.sql.delimiter.blank"; //$NON-NLS-1$

    public static final String MEMORY_CONTENT_MAX_SIZE = "content.memory.maxsize"; //$NON-NLS-1$
    public static final String CONTENT_HEX_ENCODING = "content.hex.encoding"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_CLOB = "content.cache.clob"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_BLOB = "content.cache.blob"; //$NON-NLS-1$
    public static final String CONTENT_CACHE_MAX_SIZE = "content.cache.maxsize"; //$NON-NLS-1$
    public static final String META_SEPARATE_CONNECTION = "database.meta.separate.connection"; //$NON-NLS-1$
    public static final String META_CASE_SENSITIVE = "database.meta.casesensitive"; //$NON-NLS-1$

    // Network
    public static final String NET_TUNNEL_PORT_MIN = "net.tunnel.port.min"; //$NON-NLS-1$
    public static final String NET_TUNNEL_PORT_MAX = "net.tunnel.port.max"; //$NON-NLS-1$

    // ResultSet
    public static final String RESULT_SET_MAX_ROWS_USE_SQL = "resultset.maxrows.sql"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_PRESENTATION = "resultset.binary.representation"; //$NON-NLS-1$
    public static final String RESULT_SET_BINARY_STRING_MAX_LEN = "resultset.binary.stringMaxLength"; //$NON-NLS-1$

    public static final String SQL_PARAMETERS_ENABLED = "sql.parameter.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_ENABLED = "sql.parameter.anonymous.enabled"; //$NON-NLS-1$
    public static final String SQL_ANONYMOUS_PARAMETERS_MARK = "sql.parameter.mark"; //$NON-NLS-1$
    public static final String SQL_NAMED_PARAMETERS_PREFIX = "sql.parameter.prefix"; //$NON-NLS-1$

    public final static String SQL_FORMAT_FORMATTER = "sql.format.formatter";
    public final static String SQL_FORMAT_KEYWORD_CASE = "sql.format.keywordCase";
    public final static String SQL_FORMAT_EXTERNAL_CMD = "sql.format.external.cmd";
    public final static String SQL_FORMAT_EXTERNAL_FILE = "sql.format.external.file";
    //public final static String SQL_FORMAT_EXTERNAL_DIR = "sql.format.external.dir";
    public final static String SQL_FORMAT_EXTERNAL_TIMEOUT = "sql.format.external.timeout";

    public static final String PLUGIN_ID = "org.jkiss.dbeaver.model";

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

    private static void initializeDefaultPreferences(DBPPreferenceStore store) {
        // Common
        PrefUtils.setDefaultPreferenceValue(store, QUERY_ROLLBACK_ON_ERROR, false);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER, SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_IGNORE_NATIVE_DELIMITER, false);
        PrefUtils.setDefaultPreferenceValue(store, SCRIPT_STATEMENT_DELIMITER_BLANK, true);

        PrefUtils.setDefaultPreferenceValue(store, MEMORY_CONTENT_MAX_SIZE, 10000);
        PrefUtils.setDefaultPreferenceValue(store, META_SEPARATE_CONNECTION, true);
        PrefUtils.setDefaultPreferenceValue(store, META_CASE_SENSITIVE, false);
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_HEX_ENCODING, GeneralUtils.getDefaultFileEncoding());
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_CLOB, false);
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_BLOB, false);
        PrefUtils.setDefaultPreferenceValue(store, CONTENT_CACHE_MAX_SIZE, 1000000);

        // Network
        PrefUtils.setDefaultPreferenceValue(store, NET_TUNNEL_PORT_MIN, 10000);
        PrefUtils.setDefaultPreferenceValue(store, NET_TUNNEL_PORT_MAX, 60000);

        // ResultSet
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_MAX_ROWS_USE_SQL, false);
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_BINARY_PRESENTATION, DBDBinaryFormatter.FORMATS[0].getId());
        PrefUtils.setDefaultPreferenceValue(store, RESULT_SET_BINARY_STRING_MAX_LEN, 32);

        // QM
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
            QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES, DBCExecutionPurpose.USER + "," + DBCExecutionPurpose.USER_FILTERED + "," + DBCExecutionPurpose.USER_SCRIPT);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, Platform.getLogFileLocation().toFile().getParent());

        // SQL
        PrefUtils.setDefaultPreferenceValue(store, SQL_PARAMETERS_ENABLED, true);
        PrefUtils.setDefaultPreferenceValue(store, SQL_ANONYMOUS_PARAMETERS_ENABLED, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_ANONYMOUS_PARAMETERS_MARK, String.valueOf(SQLConstants.DEFAULT_PARAMETER_MARK));
        PrefUtils.setDefaultPreferenceValue(store, SQL_NAMED_PARAMETERS_PREFIX, String.valueOf(SQLConstants.DEFAULT_PARAMETER_PREFIX));

        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_FORMATTER, SQLTokenizedFormatter.FORMATTER_ID);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_KEYWORD_CASE, "");
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_CMD, "");
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, SQL_FORMAT_EXTERNAL_TIMEOUT, 500);
    }
}
