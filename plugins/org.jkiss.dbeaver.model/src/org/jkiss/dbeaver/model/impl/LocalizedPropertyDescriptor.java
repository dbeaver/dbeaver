/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.DBPObjectWithDescriptionLocalized;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.osgi.framework.Bundle;

public abstract class LocalizedPropertyDescriptor extends PropertyDescriptor implements DBPNamedObjectLocalized, DBPObjectWithDescriptionLocalized {

    private transient final Bundle bundle;

    public LocalizedPropertyDescriptor(String category, IConfigurationElement config) {
        super(category, config);
        bundle = getBundle(config);
    }

    @Override
    public String getLocalizedName(String locale) {
        try {
            return RuntimeUtils.getBundleLocalization(bundle, locale).getString(getPropertyId());
        } catch (Exception e) {
            return this.getName();
        }
    }

    @Nullable
    @Override
    public String getLocalizedDescription(String locale) {
        try {
            return RuntimeUtils.getBundleLocalization(bundle, locale).getString(getPropertyId() + ".description");
        } catch (Exception e) {
            return this.getDescription();
        }
    }

    public String getPropertyId() {
        return this.getId();
    }

    @NotNull
    private Bundle getBundle(@NotNull IConfigurationElement config) {
        final Bundle bundle;
        String bundleName = config.getContributor().getName();
        bundle = Platform.getBundle(bundleName);
        if (bundle == null) {
            throw new IllegalStateException("Bundle '" + bundleName + "' not found");
        }
        return bundle;
    }
}
