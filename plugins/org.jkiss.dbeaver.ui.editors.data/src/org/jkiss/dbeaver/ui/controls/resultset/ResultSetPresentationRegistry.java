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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ResultSetPanelDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ResultSetPresentationRegistry
 */
public class ResultSetPresentationRegistry {

    private static final String TAG_PRESENTATION = "presentation"; //NON-NLS-1
    private static final String TAG_PANEL = "panel"; //NON-NLS-1
    private static final String TAG_OPEN_WITH = "openWith"; //NON-NLS-1

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
        presentations.sort(Comparator.comparingInt(ResultSetPresentationDescriptor::getOrder));

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

    public List<ResultSetPresentationDescriptor> getAllPresentations()
    {
        return presentations;
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

    public ResultSetPanelDescriptor getPanel(String panelId) {
        for (ResultSetPanelDescriptor panel : panels) {
            if (panel.getId().equals(panelId)) {
                return panel;
            }
        }
        return null;
    }

    public List<ResultSetPanelDescriptor> getSupportedPanels(IResultSetContext context, DBPDataSource dataSource, String presentationId, IResultSetPresentation.PresentationType presentationType) {
        List<ResultSetPanelDescriptor> result = new ArrayList<>();
        for (ResultSetPanelDescriptor panel : panels) {
            if (panel.supportedBy(context, dataSource, presentationId, presentationType)) {
                result.add(panel);
            }
        }
        result.sort(Comparator.comparing(ResultSetPanelDescriptor::getLabel));
        return result;
    }

}
