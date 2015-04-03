/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    private List<ResultSetPresentationDescriptor> presentations = new ArrayList<ResultSetPresentationDescriptor>();


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
        List<ResultSetPresentationDescriptor> result = new ArrayList<ResultSetPresentationDescriptor>();
        for (ResultSetPresentationDescriptor descriptor : presentations) {
            if (descriptor.supportedBy(resultSet, context)) {
                result.add(descriptor);
            }
        }
        return result;
    }


}
