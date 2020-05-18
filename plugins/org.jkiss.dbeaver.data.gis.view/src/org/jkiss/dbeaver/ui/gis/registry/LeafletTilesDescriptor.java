/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;

public class LeafletTilesDescriptor extends AbstractDescriptor implements DBPNamedObject  {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.data.gis.leaflet.tiles"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String layersDefinition;

    LeafletTilesDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.layersDefinition = config.getAttribute("layersDefinition");
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLayersDefinition() {
        return layersDefinition;
    }

    @NotNull
    @Override
    public String getName() {
        return id;
    }
}
