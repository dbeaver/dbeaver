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
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCResultSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ResultSetPresentationRegistry
 */
public class ResultSetPresentationRegistry {

    private static final String TAG_PRESENTATION = "presentation"; //NON-NLS-1
    private static final String TAG_PANEL = "panel"; //NON-NLS-1

    private static ResultSetPresentationRegistry instance;

    public synchronized static ResultSetPresentationRegistry getInstance() {
        if (instance == null) {
            instance = new ResultSetPresentationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<ResultSetPresentationDescriptor> presentations = new ArrayList<>();
    private List<ResultSetPanelDescriptor> panels = new ArrayList<>();

    private ResultSetPresentationRegistry(IExtensionRegistry registry)
    {
        // Load presentation descriptors
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResultSetPresentationDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (TAG_PRESENTATION.equals(ext.getName())) {
                ResultSetPresentationDescriptor descriptor = new ResultSetPresentationDescriptor(ext);
                presentations.add(descriptor);
            }
        }
        Collections.sort(presentations, new Comparator<ResultSetPresentationDescriptor>() {
            @Override
            public int compare(ResultSetPresentationDescriptor o1, ResultSetPresentationDescriptor o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });

        // Load panel descriptors
        IConfigurationElement[] panelElements = registry.getConfigurationElementsFor(ResultSetPanelDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : panelElements) {
            if (TAG_PANEL.equals(ext.getName())) {
                ResultSetPanelDescriptor descriptor = new ResultSetPanelDescriptor(ext);
                panels.add(descriptor);
            }
        }
    }

    public ResultSetPresentationDescriptor getPresentation(String id)
    {
        for (ResultSetPresentationDescriptor descriptor : presentations) {
            if (descriptor.getId().equals(id)) {
                return descriptor;
            }
        }
        return null;
    }

    public ResultSetPresentationDescriptor getPresentation(Class<? extends IResultSetPresentation> implType)
    {
        for (ResultSetPresentationDescriptor descriptor : presentations) {
            if (descriptor.matches(implType)) {
                return descriptor;
            }
        }
        return null;
    }

    public List<ResultSetPresentationDescriptor> getAvailablePresentations(DBCResultSet resultSet, IResultSetContext context)
    {
        List<ResultSetPresentationDescriptor> result = new ArrayList<>();
        for (ResultSetPresentationDescriptor descriptor : presentations) {
            if (descriptor.supportedBy(resultSet, context)) {
                result.add(descriptor);
            }
        }
        return result;
    }

    public List<ResultSetPanelDescriptor> getAllPanels() {
        return panels;
    }

    public List<ResultSetPanelDescriptor> getSupportedPanels(DBPDataSource dataSource, ResultSetPresentationDescriptor presentation) {
        List<ResultSetPanelDescriptor> result = new ArrayList<>();
        for (ResultSetPanelDescriptor panel : panels) {
            if (panel.supportedBy(dataSource, presentation)) {
                result.add(panel);
            }
        }
        Collections.sort(result, new Comparator<ResultSetPanelDescriptor>() {
            @Override
            public int compare(ResultSetPanelDescriptor o1, ResultSetPanelDescriptor o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        return result;
    }

}
