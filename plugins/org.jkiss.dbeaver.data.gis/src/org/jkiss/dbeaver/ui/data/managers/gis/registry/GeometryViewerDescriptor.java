/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.gis.IGeometryViewer;
import org.jkiss.utils.CommonUtils;

public class GeometryViewerDescriptor extends AbstractDescriptor {

    private static final Log log = Log.getLog(GeometryViewerDescriptor.class);

    private final ObjectType type;
    private final String id;
    private final String label;
    private final DBPImage icon;
    private boolean supportsInline;

    GeometryViewerDescriptor(IConfigurationElement config) {
        super(config);
        this.type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.supportsInline = CommonUtils.getBoolean(config.getAttribute("supportsInline"), false);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
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
}
