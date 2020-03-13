/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.ext.postgresql.debug.internal.impl.PostgreDebugBreakpointDescriptor;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;

public class PostgreBreakpointAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBGBreakpointDescriptor.class };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGBreakpointDescriptor.class) {
            if (adaptableObject instanceof PostgreProcedure) {
                return adapterType.cast(new PostgreDebugBreakpointDescriptor(
                    ((PostgreProcedure) adaptableObject).getObjectId(), -1
                ));
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
