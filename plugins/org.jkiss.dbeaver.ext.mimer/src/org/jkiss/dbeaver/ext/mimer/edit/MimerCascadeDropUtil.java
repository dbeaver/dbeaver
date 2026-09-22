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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.internal.MimerMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a {@code DROP ...} persist action that honours the delete-confirmation dialog's
 * "Cascade" checkbox ({@code FEATURE_DELETE_CASCADE} on the manager). When Cascade is ticked the
 * action appends {@code CASCADE} and - because that can silently drop a whole tree of dependent
 * objects - confirms once more in {@code beforeExecute}, aborting the delete on decline (same
 * abort-via-throw pattern as {@link MimerFileRenameUtil}).
 * <p>
 * That second confirmation carries a "Don't ask again" checkbox. Ticking it stores a global
 * preference (per object type - {@link MimerConstants#PREF_DROP_CASCADE_CONFIRM_PREFIX}) that
 * suppresses the prompt for that type from then on; the Mimer SQL preference page lists and
 * resets them. <b>Production connections</b> - those whose connection type has "Confirm SQL
 * execution" set ({@link org.jkiss.dbeaver.model.connection.DBPConnectionType#isConfirmExecute()},
 * on for the built-in Production type) - never see the checkbox and always get the prompt.
 *
 * @author Mimer Information Technology
 */
final class MimerCascadeDropUtil {

    private static final Log log = Log.getLog(MimerCascadeDropUtil.class);

    private MimerCascadeDropUtil() {
    }

    /** Whether the delete-confirmation dialog's "Cascade" box was checked. */
    static boolean isCascade(@Nullable Map<String, Object> options) {
        return CommonUtils.getOption(options, DBEObjectManager.OPTION_DELETE_CASCADE);
    }

    /** The per-object-type key under {@link MimerConstants#PREF_DROP_CASCADE_CONFIRM_PREFIX}. */
    @NotNull
    static String typeKey(@NotNull String typeLabel) {
        return typeLabel.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }

    /**
     * @param dropPrefix everything up to but not including the optional {@code CASCADE} keyword,
     *                   e.g. {@code DROP TABLE "s"."t"}.
     * @param typeLabel  lowercase object-type word for the confirmation text and preference key,
     *                   e.g. {@code "table"}.
     */
    @NotNull
    static SQLDatabasePersistAction dropAction(
        @NotNull String title,
        @NotNull String dropPrefix,
        @Nullable Map<String, Object> options,
        @NotNull String typeLabel,
        @NotNull String objectName,
        @NotNull DBCExecutionContext executionContext
    ) {
        boolean cascade = isCascade(options);
        return confirmedDrop(
            title, cascade ? dropPrefix + " CASCADE" : dropPrefix, cascade, typeLabel, objectName, executionContext);
    }

    /**
     * As {@link #dropAction} but for a manager that builds the whole {@code DROP} text itself
     * (e.g. one whose grammar spells out {@code RESTRICT}/{@code CASCADE} explicitly) - pass the
     * finished {@code script} and whether it's a cascading drop.
     */
    @NotNull
    static SQLDatabasePersistAction confirmedDrop(
        @NotNull String title,
        @NotNull String script,
        boolean cascade,
        @NotNull String typeLabel,
        @NotNull String objectName,
        @NotNull DBCExecutionContext executionContext
    ) {
        if (!cascade) {
            return new SQLDatabasePersistAction(title, script);
        }
        DBPDataSourceContainer container = executionContext.getDataSource().getContainer();
        // A "production" connection type (its "Confirm SQL execution" flag) always gets the extra
        // prompt - the "Don't ask again" checkbox is neither offered nor honoured there.
        boolean productionConnection = container.getConnectionConfiguration().getConnectionType().isConfirmExecute();
        String prefKey = MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + typeKey(typeLabel);
        DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();

        if (!productionConnection && prefStore.getBoolean(prefKey)) {
            // "Don't ask again" was ticked earlier for this object type.
            return new SQLDatabasePersistAction(title, script);
        }

        return new SQLDatabasePersistAction(title, script) {
            @Override
            public void beforeExecute(@NotNull DBCSession session) throws DBCException {
                List<String> rememberOption = productionConnection
                    ? List.of()
                    : List.of(NLS.bind(MimerMessages.action_cascade_drop_remember_option, typeLabel));
                DBPPlatformUI.UserChoiceResponse response = DBWorkbench.getPlatformUI().showUserChoice(
                    MimerMessages.action_cascade_drop_title,
                    NLS.bind(MimerMessages.action_cascade_drop_message, typeLabel, objectName),
                    List.of(MimerMessages.action_cascade_drop_confirm_choice, MimerMessages.action_cascade_drop_cancel_choice),
                    rememberOption,
                    1,  // focus / default: Cancel
                    1); // headless (no UI): Cancel
                if (response.choiceIndex != 0) {
                    throw new DBCException(
                        "Cancelled - the " + typeLabel + " \"" + objectName + "\" was not dropped.");
                }
                if (response.forAllChoiceIndex != null) {
                    prefStore.setValue(prefKey, true);
                    try {
                        prefStore.save();
                    } catch (IOException e) {
                        log.debug("Can't persist the CASCADE-drop confirmation preference", e);
                    }
                }
                super.beforeExecute(session);
            }
        };
    }
}
