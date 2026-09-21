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
package org.jkiss.dbeaver.model.config;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

import java.util.Optional;

public final class ProductConfigFeatureDescriptor extends AbstractDescriptor {
    private final String id;
    private final String label;
    private final String description;
    private final boolean enabledByDefault;
    private final ObjectType enablementTesterType;
    private final ObjectType availabilityTesterType;
    private ProductConfigFeatureTester enablementTester;
    private ProductConfigFeatureAvailabilityTester availabilityTester;

    ProductConfigFeatureDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.enabledByDefault = Boolean.parseBoolean(config.getAttribute("enabledByDefault"));
        this.enablementTesterType = Optional.ofNullable(config.getAttribute("enablementTester"))
            .map(ObjectType::new)
            .orElse(null);
        this.availabilityTesterType = Optional.ofNullable(config.getAttribute("availabilityTester"))
            .map(ObjectType::new)
            .orElse(null);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @Nullable
    public ProductConfigFeatureTester getEnablementTester() throws DBException {
        if (enablementTesterType == null) {
            return null;
        }
        if (enablementTester == null) {
            enablementTester = enablementTesterType.createInstance(ProductConfigFeatureTester.class);
        }
        return enablementTester;
    }

    @Nullable
    public ProductConfigFeatureAvailabilityTester getAvailabilityTester() throws DBException {
        if (availabilityTesterType == null) {
            return null;
        }
        if (availabilityTester == null) {
            availabilityTester = availabilityTesterType.createInstance(ProductConfigFeatureAvailabilityTester.class);
        }
        return availabilityTester;
    }
}
