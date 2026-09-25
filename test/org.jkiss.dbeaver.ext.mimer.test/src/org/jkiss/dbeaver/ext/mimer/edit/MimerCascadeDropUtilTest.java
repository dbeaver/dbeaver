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

import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * {@link MimerCascadeDropUtil} - the {@code CASCADE}-append text assembly, the per-object-type
 * preference key normalisation, and whether the extra confirmation is armed / suppressed / forced
 * depending on the "Don't ask again" preference and the connection type. The dialog itself
 * ({@code showUserChoice} at execute time) isn't invoked - the tests only inspect the returned
 * persist action's script and whether it carries a {@code beforeExecute} override.
 *
 * @author Mimer Information Technology
 */
public class MimerCascadeDropUtilTest extends DBeaverUnitTest {

    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private DBPDataSource dataSource;
    @Mock
    private DBPDataSourceContainer container;
    @Mock
    private DBPConnectionConfiguration connectionConfiguration;

    private DBPPreferenceStore prefStore;

    private static Map<String, Object> cascadeOption(boolean cascade) {
        Map<String, Object> options = new HashMap<>();
        options.put(DBEObjectManager.OPTION_DELETE_CASCADE, cascade);
        return options;
    }

    @BeforeEach
    public void wireConnection() {
        lenient().when(executionContext.getDataSource()).thenReturn(dataSource);
        lenient().when(dataSource.getContainer()).thenReturn(container);
        lenient().when(container.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        lenient().when(connectionConfiguration.getConnectionType()).thenReturn(DBPConnectionType.DEV);
        prefStore = DBWorkbench.getPlatform().getPreferenceStore();
    }

    @AfterEach
    public void resetPrefs() {
        for (String typeKey : MimerConstants.DROP_CASCADE_CONFIRM_TYPES.keySet()) {
            prefStore.setToDefault(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + typeKey);
        }
    }

    private static boolean isConfirmationArmed(SQLDatabasePersistAction action) {
        // confirmedDrop() returns a plain SQLDatabasePersistAction when no prompt is needed and an
        // anonymous subclass (with a beforeExecute override) when it is.
        return action.getClass() != SQLDatabasePersistAction.class;
    }

    @Test
    public void isCascadeReadsTheOption() {
        Assertions.assertTrue(MimerCascadeDropUtil.isCascade(cascadeOption(true)));
        Assertions.assertFalse(MimerCascadeDropUtil.isCascade(cascadeOption(false)));
        Assertions.assertFalse(MimerCascadeDropUtil.isCascade(new HashMap<>()));
        Assertions.assertFalse(MimerCascadeDropUtil.isCascade(null));
    }

    @Test
    public void typeKeyLowercasesAndUnderscoresSpaces() {
        Assertions.assertEquals("table", MimerCascadeDropUtil.typeKey("table"));
        Assertions.assertEquals("method_specification", MimerCascadeDropUtil.typeKey("method specification"));
        Assertions.assertEquals("user", MimerCascadeDropUtil.typeKey(" User "));
        // Every label passed at a call site must map to a key the preference page knows.
        Assertions.assertTrue(MimerConstants.DROP_CASCADE_CONFIRM_TYPES.containsKey(
            MimerCascadeDropUtil.typeKey("method specification")));
    }

    @Test
    public void noCascadeMeansPlainActionAndNoPrompt() {
        SQLDatabasePersistAction plain = MimerCascadeDropUtil.dropAction(
            "Drop table", "DROP TABLE \"s\".\"t\"", cascadeOption(false), "table", "t", executionContext);
        Assertions.assertEquals("DROP TABLE \"s\".\"t\"", plain.getScript());
        Assertions.assertFalse(isConfirmationArmed(plain));
    }

    @Test
    public void cascadeAppendsKeywordAndArmsTheConfirmation() {
        SQLDatabasePersistAction cascaded = MimerCascadeDropUtil.dropAction(
            "Drop table", "DROP TABLE \"s\".\"t\"", cascadeOption(true), "table", "t", executionContext);
        Assertions.assertEquals("DROP TABLE \"s\".\"t\" CASCADE", cascaded.getScript());
        Assertions.assertTrue(isConfirmationArmed(cascaded));
    }

    @Test
    public void dontAskAgainPreferenceSuppressesTheConfirmationOnANonProductionConnection() {
        prefStore.setValue(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + "table", true);

        SQLDatabasePersistAction action = MimerCascadeDropUtil.dropAction(
            "Drop table", "DROP TABLE \"s\".\"t\"", cascadeOption(true), "table", "t", executionContext);

        Assertions.assertEquals("DROP TABLE \"s\".\"t\" CASCADE", action.getScript());
        Assertions.assertFalse(isConfirmationArmed(action), "the 'don't ask again' preference should suppress the prompt");
    }

    @Test
    public void theSuppressionPreferenceIsPerObjectType() {
        prefStore.setValue(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + "table", true);

        SQLDatabasePersistAction schemaDrop = MimerCascadeDropUtil.dropAction(
            "Drop schema", "DROP SCHEMA \"s\"", cascadeOption(true), "schema", "s", executionContext);

        Assertions.assertTrue(isConfirmationArmed(schemaDrop), "suppressing 'table' must not suppress 'schema'");
    }

    @Test
    public void productionConnectionAlwaysConfirmsEvenWithThePreferenceSet() {
        when(connectionConfiguration.getConnectionType()).thenReturn(DBPConnectionType.PROD);
        prefStore.setValue(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + "table", true);

        SQLDatabasePersistAction action = MimerCascadeDropUtil.dropAction(
            "Drop table", "DROP TABLE \"s\".\"t\"", cascadeOption(true), "table", "t", executionContext);

        Assertions.assertTrue(isConfirmationArmed(action),
            "a production connection must confirm regardless of the suppression preference");
    }

    @Test
    public void confirmedDropPassesTheScriptThroughVerbatim() {
        SQLDatabasePersistAction restrict = MimerCascadeDropUtil.confirmedDrop(
            "Drop method", "DROP SPECIFIC METHOD \"m\" RESTRICT", false, "method", "m", executionContext);
        Assertions.assertEquals("DROP SPECIFIC METHOD \"m\" RESTRICT", restrict.getScript());
        Assertions.assertFalse(isConfirmationArmed(restrict));

        SQLDatabasePersistAction cascade = MimerCascadeDropUtil.confirmedDrop(
            "Drop method", "DROP SPECIFIC METHOD \"m\" CASCADE", true, "method", "m", executionContext);
        Assertions.assertEquals("DROP SPECIFIC METHOD \"m\" CASCADE", cascade.getScript());
        Assertions.assertTrue(isConfirmationArmed(cascade));
    }
}
