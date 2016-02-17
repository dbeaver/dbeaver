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

    private static ResultSetPresentationRegistry instance;

    public synchronized static ResultSetPresentationRegistry getInstance() {
        if (instance == null) {
            instance = new ResultSetPresentationRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private List<ResultSetPresentationDescriptor> presentations = new ArrayList<>();


    private ResultSetPresentationRegistry(IExtensionRegistry registry)
    {
        // Load datasource providers from external plugins
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
    }

    public ResultSetPresentationDescriptor getDescriptor(Class<? extends IResultSetPresentation> implType)
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


}
