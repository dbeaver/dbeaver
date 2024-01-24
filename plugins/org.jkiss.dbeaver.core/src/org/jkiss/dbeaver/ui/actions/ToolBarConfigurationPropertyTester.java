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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.ui.ActionUtils;

public class ToolBarConfigurationPropertyTester extends PropertyTester {
    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.toolbar.configuration"; //$NON-NLS-1$
    public static final String PROP_VISIBLE = "visible"; //$NON-NLS-1$

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (PROP_VISIBLE.equals(property) && args != null && args.length == 2) {
            return ToolBarConfigurationRegistry.getInstance().isItemVisible(args[0].toString(), args[1].toString());
        }
        return true;
    }

    /**
     * Notify eclipse that visibility preferences were changed
     */
    public static void fireVisibilityPropertyChange() {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + PROP_VISIBLE);
    }
}
