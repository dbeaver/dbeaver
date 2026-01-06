/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.Objects;

public final class ResultSetErrorActionDescriptor extends AbstractDescriptor {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.error"; //$NON-NLS-1$

    private final String label;
    private final String description;
    private final DBPImage icon;
    private final ObjectType action;
    private final int order;

    ResultSetErrorActionDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = Objects.requireNonNull(iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON)));
        this.action = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public DBPImage getIcon() {
        return icon;
    }

    public int getOrder() {
        return order;
    }

    @NotNull
    public IResultSetErrorAction createInstance() throws DBException {
        return action.createInstance(IResultSetErrorAction.class);
    }
}
