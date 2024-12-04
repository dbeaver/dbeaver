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
package org.jkiss.dbeaver.registry.settings;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.DBPObjectWithDescriptionLocalized;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProductSettingDescriptor extends PropertyDescriptor implements DBPNamedObjectLocalized, DBPObjectWithDescriptionLocalized {
    private final List<String> scopes = new ArrayList<>();
    private final Bundle bundle;

    public ProductSettingDescriptor(String category, IConfigurationElement cfg) {
        super(category, cfg);
        String excludeAttr = cfg.getAttribute("scopes");
        bundle = FrameworkUtil.getBundle(getClass()).getBundleContext()
            .getBundle(Long.parseLong(((RegistryContributor)cfg.getContributor()).getActualId()));
        if (CommonUtils.isNotEmpty(excludeAttr)) {
            scopes.addAll(Arrays.stream(excludeAttr.split(",")).toList());
        }
    }

    @NotNull
    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public String getLocalizedName(String locale) {
        try {
            return RuntimeUtils.getBundleLocalization(bundle, locale).getString(this.getId());
        } catch (Exception e) {
            return this.getName();
        }
    }

    @Nullable
    @Override
    public String getLocalizedDescription(String locale) {
        try {
            return RuntimeUtils.getBundleLocalization(bundle, locale).getString(this.getId() + ".description");
        } catch (Exception e) {
            return this.getDescription();
        }
    }


}
