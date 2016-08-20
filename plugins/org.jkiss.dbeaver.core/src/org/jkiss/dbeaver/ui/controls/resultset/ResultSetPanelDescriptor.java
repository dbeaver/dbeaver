/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ResultSetPresentationDescriptor
 */
public class ResultSetPanelDescriptor extends AbstractContextDescriptor {

    private static final Log log = Log.getLog(ResultSetPanelDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resultset.panel"; //NON-NLS-1 //$NON-NLS-1$

    public static final String TAG_SUPPORTS = "supports"; //$NON-NLS-1$

    private final String id;
    private final ObjectType implClass;
    private final List<IResultSetPresentation.PresentationType> supportedPresentationTypes = new ArrayList<>();
    private final List<String> supportedPresentations = new ArrayList<>();

    protected ResultSetPanelDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.implClass = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));

        for (IConfigurationElement supports : config.getChildren(TAG_SUPPORTS)) {
            String type = supports.getAttribute(RegistryConstants.ATTR_TYPE);
            if (!CommonUtils.isEmpty(type)) {
                supportedPresentationTypes.add(IResultSetPresentation.PresentationType.valueOf(type.toUpperCase(Locale.ENGLISH)));
            }
            String id = supports.getAttribute(RegistryConstants.ATTR_ID);
            if (!CommonUtils.isEmpty(id)) {
                supportedPresentations.add(id);
            }
        }
    }

    public String getId() {
        return id;
    }

    public boolean supportedBy(ResultSetPresentationDescriptor presentation) {
        return
            supportedPresentations.contains(presentation.getId()) ||
            supportedPresentationTypes.contains(presentation.getPresentationType());
    }

    public IResultSetPanel createInstance() throws DBException {
        return implClass.createInstance(IResultSetPanel.class);
    }

    @Override
    public String toString() {
        return id;
    }
}
