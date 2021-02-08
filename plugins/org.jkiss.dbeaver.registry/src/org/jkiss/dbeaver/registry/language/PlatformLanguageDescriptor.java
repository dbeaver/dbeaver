/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.language;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObjectLocalized;
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * PlatformLanguageDescriptor
 */
public class PlatformLanguageDescriptor extends AbstractContextDescriptor implements DBPPlatformLanguage, DBPNamedObjectLocalized {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.language"; //$NON-NLS-1$

    private final IConfigurationElement config;

    public PlatformLanguageDescriptor(IConfigurationElement config) {
        super(config);

        this.config = config;
    }

    @NotNull
    @Override
    public String getCode() {
        return config.getAttribute(RegistryConstants.ATTR_CODE);
    }

    @NotNull
    @Override
    public String getLabel() {
        return config.getAttribute(RegistryConstants.ATTR_LABEL);
    }

    public String getDescription() {
        return config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
    }

    @Override
    public String toString() {
        return getCode();
    }

    @NotNull
    @Override
    public String getName() {
        return getLabel();
    }

    @Override
    public String getLocalizedName(String locale) {
        return config.getAttribute(RegistryConstants.ATTR_LABEL, locale);
    }

}
