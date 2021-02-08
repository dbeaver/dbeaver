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
package org.jkiss.dbeaver.ui.internal.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;
import org.jkiss.utils.CommonUtils;

/**
 * NavigatorNodeActionDescriptor
 */
public class NavigatorNodeActionDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.navigator.nodeAction"; //NON-NLS-1 //$NON-NLS-1$

    private ObjectType implType;
    private int order;
    private INavigatorNodeActionHandler instance;

    NavigatorNodeActionDescriptor(IConfigurationElement config) throws DBException {
        super(config);

        this.implType = new ObjectType(config.getAttribute("class"));
        this.instance = implType.createInstance(INavigatorNodeActionHandler.class);
        this.order = CommonUtils.toInt(config.getAttribute("order"));
    }

    @NotNull
    public INavigatorNodeActionHandler getHandler() {
        return instance;
    }

    public int getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return implType.getImplName();
    }

}
