/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

public class DBeaverCoreAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBPPlatform.class, DBPPlatformUI.class };
    
    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof DBWorkbench) {
            if (adapterType == DBPPlatform.class) {
                return adapterType.cast(DBeaverCore.getInstance());
            } else if (adapterType == DBPPlatformUI.class) {
                return adapterType.cast(DBeaverUI.getInstance());
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
