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
package org.jkiss.dbeaver.ui.internal.registry;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;
import org.jkiss.utils.CommonUtils;

/**
 * NavigatorNodeActionDescriptor
 */
public class NavigatorNodeActionDescriptor extends AbstractContextDescriptor {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.navigator.nodeAction"; //NON-NLS-1 //$NON-NLS-1$

    private final ObjectType implType;
    private final int order;
    private final Expression enablementExpression;
    private INavigatorNodeActionHandler instance;

    NavigatorNodeActionDescriptor(IConfigurationElement config) {
        super(config);

        this.implType = new ObjectType(config.getAttribute("class"));
        this.order = CommonUtils.toInt(config.getAttribute("order"));
        this.enablementExpression = getEnablementExpression(config);
    }

    @NotNull
    public INavigatorNodeActionHandler getHandler() {
        if (instance == null) {
            try {
                this.instance = implType.createInstance(INavigatorNodeActionHandler.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    public int getOrder() {
        return order;
    }

    public boolean appliesTo(DBPObject object) {
        return isExpressionTrue(enablementExpression, object) && appliesTo(object, null);
    }

    @Override
    public String toString() {
        return implType.getImplName();
    }

}
