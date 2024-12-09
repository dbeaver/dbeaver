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
package org.jkiss.dbeaver.registry.data.hints;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.utils.CommonUtils;

/**
 * ValueHintProviderDescriptor
 */
public class ValueHintProviderDescriptor extends AbstractValueBindingDescriptor<DBDValueHintProvider> {
    private static final Log log = Log.getLog(ValueHintProviderDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dataHintProvider"; //$NON-NLS-1$
    public static final String TAG_HINT_PROVIDER = "hintProvider"; //$NON-NLS-1$

    private final boolean visibleByDefault;
    private final String label;

    public ValueHintProviderDescriptor(IConfigurationElement config) {
        super(config);
        this.visibleByDefault = CommonUtils.getBoolean(config.getAttribute("visibleByDefault"), true);
        this.label = config.getAttribute("label");
    }

    @Override
    protected Class<DBDValueHintProvider> getImplClass() {
        return DBDValueHintProvider.class;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isEnabled() {
        return ValueHintRegistry.getInstance().isHintEnabled(this);
    }

    public boolean isVisibleByDefault() {
        return visibleByDefault;
    }
}