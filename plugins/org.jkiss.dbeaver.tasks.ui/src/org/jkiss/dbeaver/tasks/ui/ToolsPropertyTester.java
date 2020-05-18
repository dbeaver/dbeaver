/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.registry.ToolDescriptor;
import org.jkiss.dbeaver.tools.registry.ToolsRegistry;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

/**
 * ToolsPropertyTester
 */
public class ToolsPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.tools";
    public static final String PROP_HAS_TOOLS = "hasTools";

    public ToolsPropertyTester() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (!(receiver instanceof IWorkbenchPart)) {
            return false;
        }
        IStructuredSelection structuredSelection = NavigatorUtils.getSelectionFromPart((IWorkbenchPart)receiver);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return false;
        }

        switch (property) {
            case PROP_HAS_TOOLS: {
                return hasAvailableTools(structuredSelection);
            }
        }
        return false;
    }

    private boolean hasAvailableTools(IStructuredSelection selection) {
        boolean singleObject = selection.size() == 1;
        for (Object item : selection.toArray()) {
            DBSObject dbObject = DBUtils.getFromObject(item);
            if (dbObject != null) {
                item = dbObject;
            }
            if (item instanceof DBPObject) {
                for (ToolDescriptor descriptor : ToolsRegistry.getInstance().getTools()) {
                    if (descriptor.isSingleton() && !singleObject) {
                        continue;
                    }
                    if (descriptor.appliesTo((DBPObject) item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
