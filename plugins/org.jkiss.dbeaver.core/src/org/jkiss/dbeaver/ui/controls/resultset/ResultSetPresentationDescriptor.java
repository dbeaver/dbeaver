/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

/**
 * ResultSetPresentationDescriptor
 */
public class ResultSetPresentationDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.presentation"; //NON-NLS-1 //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType presentationType;
    private final DBPImage icon;
    private final int order;

    protected ResultSetPresentationDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.label = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.presentationType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        this.order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER));
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

    public int getOrder() {
        return order;
    }

    public boolean supportedBy(DBCResultSet resultSet, IResultSetContext context) {
        return appliesTo(resultSet, context);
    }

    public IResultSetPresentation createInstance() throws DBException {
        return presentationType.createInstance(IResultSetPresentation.class);
    }

    public boolean matches(Class<? extends IResultSetPresentation> type) {
        return presentationType.matchesType(type);
    }
}
