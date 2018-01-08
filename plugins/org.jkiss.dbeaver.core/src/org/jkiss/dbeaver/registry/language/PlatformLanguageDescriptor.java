/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.app.DBPPlatformLanguage;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

/**
 * PlatformLanguageDescriptor
 */
public class PlatformLanguageDescriptor extends AbstractContextDescriptor implements DBPPlatformLanguage
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.language"; //$NON-NLS-1$

    private final String code;
    private final String label;
    private final String description;

    public PlatformLanguageDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.code = config.getAttribute(RegistryConstants.ATTR_CODE);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
    }

    @NotNull
    @Override
    public String getCode() {
        return code;
    }

    @NotNull
    @Override
    public String getLabel()
    {
        return label;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }
}
