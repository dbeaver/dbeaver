/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.bundle;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.Arrays;

public class ModelPreferencesInitializer extends AbstractPreferenceInitializer {

    public ModelPreferencesInitializer() {
    }

    @Override
    public void initializeDefaultPreferences() {
        // Init default preferences
        DBPPreferenceStore store = new BundlePreferenceStore(ModelActivator.getInstance().getBundle());

        // Common
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.QUERY_ROLLBACK_ON_ERROR, false);

        // SQL execution
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_STATEMENT_DELIMITER, SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.SCRIPT_IGNORE_NATIVE_DELIMITER, false);

        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.MEMORY_CONTENT_MAX_SIZE, 10000);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.META_SEPARATE_CONNECTION, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.META_CASE_SENSITIVE, false);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.CONTENT_HEX_ENCODING, GeneralUtils.getDefaultFileEncoding());

        // Network
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NET_TUNNEL_PORT_MIN, 10000);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.NET_TUNNEL_PORT_MAX, 60000);

        // ResultSet
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL, true);
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_PRESENTATION, DBDBinaryFormatter.FORMATS[0].getId());
        PrefUtils.setDefaultPreferenceValue(store, ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN, 32);

        // QM
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_HISTORY_DAYS, 90);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_ENTRIES_PER_PAGE, 200);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_OBJECT_TYPES,
            QMObjectType.toString(Arrays.asList(QMObjectType.txn, QMObjectType.query)));
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_QUERY_TYPES, DBCExecutionPurpose.USER + "," + DBCExecutionPurpose.USER_SCRIPT);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_STORE_LOG_FILE, false);
        PrefUtils.setDefaultPreferenceValue(store, QMConstants.PROP_LOG_DIRECTORY, Platform.getLogFileLocation().toFile().getParent());
    }

}
