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

import org.jkiss.code.NotNull;

/**
 * Determines the initial enablement state of a feature.
 * <p>
 * For example, a feature might be considered explicitly enabled
 * by the user if they have a related preference key set.
 * <p>
 * If enablement is {@link Enablement#UNDEFINED undefined}, then
 * the feature's {@link ProductConfigFeatureDescriptor#isEnabledByDefault()
 * default enablement} property will be used to determine its initial state.
 */
public interface ProductConfigFeatureTester {
    enum Enablement {
        EXPLICITLY_ENABLED,
        EXPLICITLY_DISABLED,
        UNDEFINED
    }

    @NotNull
    Enablement isFeatureEnabled();
}
