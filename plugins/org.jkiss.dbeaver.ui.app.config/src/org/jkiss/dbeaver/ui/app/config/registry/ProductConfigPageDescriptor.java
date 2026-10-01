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
package org.jkiss.dbeaver.ui.app.config.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.app.config.pages.ProductConfigWizardPage;

public final class ProductConfigPageDescriptor extends AbstractDescriptor {
    private final String id;
    private final int order;
    private final ObjectType type;

    ProductConfigPageDescriptor(@NotNull IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.order = Integer.parseInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
        this.type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
    }

    @NotNull
    public String getId() {
        return id;
    }

    public int getOrder() {
        return order;
    }

    @NotNull
    public ProductConfigWizardPage createPage() throws DBException {
        return type.createInstance(ProductConfigWizardPage.class);
    }
}
