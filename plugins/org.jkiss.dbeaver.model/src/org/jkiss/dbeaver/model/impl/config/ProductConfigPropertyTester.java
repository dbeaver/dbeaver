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
package org.jkiss.dbeaver.model.impl.config;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureRegistry;

public final class ProductConfigPropertyTester extends PropertyTester {
    private static final Log log = Log.getLog(ProductConfigPropertyTester.class);

    private static final String PROP_IS_FEATURE_ENABLED = "isFeatureEnabled";

    @Override
    public boolean test(@Nullable Object receiver, @NotNull String property, @NotNull Object[] args, @Nullable Object expectedValue) {
        if (!PROP_IS_FEATURE_ENABLED.equals(property)) {
            return false;
        }
        if (!ProductConfigUtils.isShowOnStartup()) {
            // Some features might be disabled by default by plugin declaration.
            // But since Product Config is an unreleased feature, there's no way to configure what's enabled or not.
            // So assume all features are enabled when the flag is absent.
            return true;
        }
        if (!(expectedValue instanceof String value)) {
            return false;
        }
        var registry = ProductConfigFeatureRegistry.getInstance();
        for (ProductConfigFeatureDescriptor feature : registry.getFeatures()) {
            if (feature.getId().equals(value)) {
                return registry.isFeatureEnabled(feature);
            }
        }
        log.debug("Unknown product config feature '" + value + "'");
        return false;
    }
}
