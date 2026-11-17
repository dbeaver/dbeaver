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

import org.jkiss.dbeaver.model.impl.config.ProductConfigPropertyTester;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public final class DDProductConfig {
    public static final String INTEGRATION_ENABLED = "datadam.integration.enabled";
    private static final ProductConfigPropertyTester PROPERTY_TESTER = new ProductConfigPropertyTester();

    private DDProductConfig() {
    }

    public static boolean isIntegrationEnabled() {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        return isIntegrationAvailable() && (!store.contains(INTEGRATION_ENABLED) || store.getBoolean(INTEGRATION_ENABLED));
    }

    public static boolean isIntegrationAvailable() {
        return PROPERTY_TESTER.test(null, "isFeatureEnabled", new Object[0], "datadam");
    }

    public static void setIntegrationEnabled(boolean enabled) {
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        // Without the effective default, the first write of false is treated as unchanged.
        store.setDefault(INTEGRATION_ENABLED, true);
        store.setValue(INTEGRATION_ENABLED, enabled);
    }
}
