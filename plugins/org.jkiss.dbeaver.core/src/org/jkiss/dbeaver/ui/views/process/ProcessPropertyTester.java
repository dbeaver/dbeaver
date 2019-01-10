/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.runtime.DBRProcessController;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * ProcessPropertyTester
 */
public class ProcessPropertyTester extends PropertyTester
{

    public static final String NAMESPACE = "org.jkiss.dbeaver.runtime.process";
    public static final String PROP_RUNNING = "running";

    public ProcessPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBRProcessController)) {
            return false;
        }
        DBRProcessController controller = (DBRProcessController)receiver;
        if (property.equals(PROP_RUNNING)) {
            return controller.getProcessDescriptor() != null && controller.getProcessDescriptor().isRunning();
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}