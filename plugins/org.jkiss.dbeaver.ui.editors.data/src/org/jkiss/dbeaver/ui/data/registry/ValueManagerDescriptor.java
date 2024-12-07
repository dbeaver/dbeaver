/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.registry.data.hints.AbstractValueBindingDescriptor;
import org.jkiss.dbeaver.ui.data.IValueManager;

/**
 * ValueManagerDescriptor
 */
public class ValueManagerDescriptor extends AbstractValueBindingDescriptor<IValueManager> {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataManager"; //$NON-NLS-1$
    public static final String TAG_MANAGER = "manager"; //$NON-NLS-1$

    private IValueManager instance;

    public ValueManagerDescriptor(IConfigurationElement config) {
        super(config);
    }

    @Override
    protected Class<IValueManager> getImplClass() {
        return IValueManager.class;
    }


}