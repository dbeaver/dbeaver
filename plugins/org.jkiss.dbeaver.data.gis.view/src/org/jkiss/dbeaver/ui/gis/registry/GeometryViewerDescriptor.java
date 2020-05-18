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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.gis.IGeometryViewer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class GeometryViewerDescriptor extends AbstractDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.data.gis.geometryViewer"; //$NON-NLS-1$

    private static final Log log = Log.getLog(GeometryViewerDescriptor.class);

    private final ObjectType type;
    private final String id;
    private final String label;
    private final String description;
    private final DBPImage icon;
    private boolean supportsInline;
    private final List<String> supportedDataSources = new ArrayList<>();

    GeometryViewerDescriptor(IConfigurationElement config) {
        super(config);
        this.type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.supportsInline = CommonUtils.getBoolean(config.getAttribute("supportsInline"), false);

        for (IConfigurationElement dsElement : config.getChildren("datasource")) {
            String dsId = dsElement.getAttribute("id");
            if (dsId != null) {
                supportedDataSources.add(dsId);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public DBPImage getIcon() {
        return icon;
    }

    public IGeometryViewer createGeometryViewer(IValueController valueController) throws DBException {
        try {
            return (IGeometryViewer) type.getObjectClass().getConstructor(IValueController.class).newInstance(valueController);
        } catch (Throwable e) {
            throw new DBException("Error instantiating geometry viewer", e);
        }
    }

    public boolean supportsInlineView() {
        return supportsInline;
    }

    public boolean supportedBy(DBPDataSource dataSource) {
        if (!supportedDataSources.isEmpty()) {
            if (dataSource == null) {
                return false;
            }
            if (!supportedDataSources.contains(dataSource.getContainer().getDriver().getProviderId())) {
                return false;
            }
        }
        return true;
    }

}
