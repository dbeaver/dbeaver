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
package org.jkiss.dbeaver.model.ai.app.config;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureAvailabilityTester;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureTester;
import org.jkiss.dbeaver.model.impl.GlobalPropertyTester;

public final class AIProductConfigFeatureTester implements ProductConfigFeatureTester, ProductConfigFeatureAvailabilityTester {
    private static final Log log = Log.getLog(AIProductConfigFeatureTester.class);
    private static final String AI_DISABLED_PROPERTY = "ai.disabled";
    private static final String AI_DISABLED_ENV_VARIABLE = "DBEAVER_AI_DISABLED";
    private static final GlobalPropertyTester GLOBAL_PROPERTY_TESTER = new GlobalPropertyTester();

    @NotNull
    @Override
    public Enablement isFeatureEnabled() {
        try {
            if (AISettingsManager.isConfigExists() &&
                AISettingsManager.getInstance().getSettings().isAiDisabled()
            ) {
                return Enablement.EXPLICITLY_DISABLED;
            }
        } catch (DBException e) {
            log.error("Error checking AI feature enablement", e);
        }
        return Enablement.UNDEFINED;
    }

    @Override
    public boolean isFeatureAvailable() {
        return !GLOBAL_PROPERTY_TESTER.test(
            this,
            GlobalPropertyTester.PROP_HAS_PREFERENCE,
            new Object[0],
            AI_DISABLED_PROPERTY
        ) && !GLOBAL_PROPERTY_TESTER.test(
            this,
            GlobalPropertyTester.PROP_HAS_ENV_VARIABLE,
            new Object[0],
            AI_DISABLED_ENV_VARIABLE
        );
    }
}
