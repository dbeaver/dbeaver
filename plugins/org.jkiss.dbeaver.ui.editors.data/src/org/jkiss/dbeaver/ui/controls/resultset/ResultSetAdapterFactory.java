/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.dialogs.IPageChangeProvider;

/**
 * ResultSetAdapterFactory
 */
public class ResultSetAdapterFactory implements IAdapterFactory
{
    private static final Class<?>[] ADAPTER_LIST = { ResultSetViewer.class };

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType)
    {
        if (adapterType == ResultSetViewer.class) {
            if (adaptableObject instanceof ResultSetViewer) {
                return adaptableObject;
            } else if (adaptableObject instanceof IResultSetContainer) {
                return ((IResultSetContainer) adaptableObject).getResultSetController();
            }
            if (adaptableObject instanceof IPageChangeProvider) {
                return getAdapter(((IPageChangeProvider) adaptableObject).getSelectedPage(), ResultSetViewer.class);
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList()
    {
        return ADAPTER_LIST;
    }
}