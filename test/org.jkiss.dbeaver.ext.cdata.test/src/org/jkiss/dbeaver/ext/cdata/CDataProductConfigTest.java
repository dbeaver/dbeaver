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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureTester;
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CDataProductConfigTest extends DBeaverUnitTest {
    @Test
    public void enabledByDefault() throws Exception {
        var feature = getFeature();

        Assertions.assertTrue(feature.isEnabledByDefault());
        Assertions.assertNull(feature.getEnablementTester());
        Assertions.assertEquals("CData integration", feature.getLabel());
    }

    @Test
    public void canDisableAndEnableCData() {
        var registry = ProductConfigRegistry.getInstance();
        var feature = getFeature();
        boolean wasEnabled = registry.isFeatureEnabled(feature);
        try {
            registry.setFeatureEnabled(feature, false);
            Assertions.assertFalse(registry.isFeatureEnabled(feature));
            Assertions.assertEquals(ProductConfigFeatureTester.Enablement.EXPLICITLY_DISABLED, registry.getFeatureEnablement(feature));

            registry.setFeatureEnabled(feature, true);
            Assertions.assertTrue(registry.isFeatureEnabled(feature));
            Assertions.assertEquals(ProductConfigFeatureTester.Enablement.EXPLICITLY_ENABLED, registry.getFeatureEnablement(feature));
        } finally {
            registry.setFeatureEnabled(feature, wasEnabled);
        }
    }

    @NotNull
    private static ProductConfigFeatureDescriptor getFeature() {
        return ProductConfigRegistry.getInstance().getFeatures().stream()
            .filter(feature -> feature.getId().equals("cdata"))
            .findFirst()
            .orElseThrow();
    }
}
