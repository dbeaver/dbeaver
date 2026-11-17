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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

class GlobalPropertyTesterTest {
    @Test
    void preferenceDefaultDoesNotOverrideExplicitFalseAndPreservesExistingDefault() {
        String preference = "statusbar.test." + UUID.randomUUID();
        var store = new SimplePreferenceStore() {
            @Override
            public void save() {
            }
        };
        var platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(store);
        try (var workbench = Mockito.mockStatic(DBWorkbench.class)) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            var tester = new GlobalPropertyTester();
            Assertions.assertFalse(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[0], preference));
            Assertions.assertTrue(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[] {true}, preference));
            store.setValue(preference, false);
            Assertions.assertFalse(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[] {true}, preference));
            store.setValue(preference, true);
            Assertions.assertTrue(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[] {false}, preference));
        }
    }

    @Test
    void systemPropertyFallbackTakesPrecedenceOverTheOptionalDefault() {
        String preference = "statusbar.test." + UUID.randomUUID();
        var store = new SimplePreferenceStore() {
            @Override
            public void save() {
            }
        };
        var platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(store);
        try (var workbench = Mockito.mockStatic(DBWorkbench.class)) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            var tester = new GlobalPropertyTester();
            System.setProperty(preference, "false");
            Assertions.assertFalse(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[] {true}, preference));
            System.setProperty(preference, "");
            Assertions.assertTrue(tester.test(null, GlobalPropertyTester.PROP_HAS_PREFERENCE, new Object[] {false}, preference));
        } finally {
            System.clearProperty(preference);
        }
    }
}
