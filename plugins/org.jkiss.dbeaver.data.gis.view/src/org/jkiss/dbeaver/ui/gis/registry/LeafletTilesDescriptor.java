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
package org.jkiss.dbeaver.ui.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;

import java.util.Objects;

public final class LeafletTilesDescriptor extends AbstractDescriptor implements DBPNamedObject  {
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.data.gis.leaflet.tiles"; //$NON-NLS-1$
    private static final String USER_DEFINED_DESCRIPTOR_ID_PREFIX = EXTENSION_ID + ".userDefinedTiles.";

    private final String id;
    private final String label;
    private final String layersDefinition;
    private final boolean isPredefined;
    private final boolean isVisible;

    private LeafletTilesDescriptor(@NotNull String id, @NotNull String label, @NotNull String layersDefinition, boolean isPredefined, boolean isVisible) {
        super(GISViewerActivator.PLUGIN_ID);
        this.id = id.trim();
        this.label = label.trim();
        this.layersDefinition = layersDefinition.trim();
        this.isPredefined = isPredefined;
        this.isVisible = isVisible;
    }

    static LeafletTilesDescriptor createPredefined(@NotNull IConfigurationElement config) {
        return new LeafletTilesDescriptor(
            config.getAttribute(RegistryConstants.ATTR_ID),
            config.getAttribute(RegistryConstants.ATTR_LABEL),
            config.getAttribute("layersDefinition"),
            true,
            true
        );
    }

    public static LeafletTilesDescriptor createUserDefined(@NotNull String label, @NotNull String layersDefinition, boolean isVisible) {
        return new LeafletTilesDescriptor(
            USER_DEFINED_DESCRIPTOR_ID_PREFIX + label,
            label,
            layersDefinition,
            false,
            isVisible
        );
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

    public boolean isPredefined() {
        return isPredefined;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public LeafletTilesDescriptor withFlippedVisibility() {
        return new LeafletTilesDescriptor(id, label, layersDefinition, isPredefined, !isVisible);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeafletTilesDescriptor that = (LeafletTilesDescriptor) o;
        return isPredefined == that.isPredefined && isVisible == that.isVisible && id.equals(that.id) && label.equals(that.label) && layersDefinition.equals(that.layersDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, layersDefinition, isPredefined, isVisible);
    }
}
