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
package org.jkiss.dbeaver.model.datadam;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DDProductConfigTest extends DBeaverUnitTest {
    @Test
    void firstDisablePersistsAndEveryToggleNotifiesListeners() throws Exception {
        String node = "org.jkiss.dbeaver.datadam.test." + UUID.randomUUID();
        var store = new BundlePreferenceStore(node);
        var platform = Mockito.mock(DBPPlatform.class);
        Mockito.when(platform.getPreferenceStore()).thenReturn(store);
        List<Object> changes = new ArrayList<>();
        store.addPropertyChangeListener(event -> {
            if (DDProductConfig.INTEGRATION_ENABLED.equals(event.getProperty())) {
                changes.add(event.getNewValue());
            }
        });
        try (var workbench = Mockito.mockStatic(DBWorkbench.class);
             var config = Mockito.mockStatic(DDProductConfig.class)) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            config.when(DDProductConfig::isIntegrationAvailable).thenReturn(true);
            config.when(DDProductConfig::isIntegrationEnabled).thenCallRealMethod();
            config.when(() -> DDProductConfig.setIntegrationEnabled(Mockito.anyBoolean())).thenCallRealMethod();
            Assertions.assertFalse(store.contains(DDProductConfig.INTEGRATION_ENABLED));
            Assertions.assertTrue(DDProductConfig.isIntegrationEnabled());

            DDProductConfig.setIntegrationEnabled(false);
            Assertions.assertFalse(DDProductConfig.isIntegrationEnabled());
            Assertions.assertFalse(new BundlePreferenceStore(node).getBoolean(DDProductConfig.INTEGRATION_ENABLED));
            DDProductConfig.setIntegrationEnabled(true);
            Assertions.assertTrue(DDProductConfig.isIntegrationEnabled());
            DDProductConfig.setIntegrationEnabled(false);
            Assertions.assertFalse(DDProductConfig.isIntegrationEnabled());
            Assertions.assertEquals(List.of(false, true, false), changes);
        } finally {
            DefaultScope.INSTANCE.getNode(node).removeNode();
            InstanceScope.INSTANCE.getNode(node).removeNode();
        }
    }
}
