/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.task.DBTScheduler;
import org.jkiss.dbeaver.registry.task.TaskRegistry;

public class SchedulerPropertyTester extends PropertyTester {
    public static final String NAMESPACE = "org.jkiss.dbeaver.task.scheduler";
    public static final String PROP_CAN_OPEN_EXTERNAL_SETTINGS = "canOpenExternalSettings";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        final DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();

        if (scheduler == null) {
            return false;
        }

        if (property.equals(PROP_CAN_OPEN_EXTERNAL_SETTINGS)) {
            return scheduler.supportsFeature(DBTScheduler.FEATURE_OPEN_EXTERNAL_SETTINGS);
        }

        return false;
    }
}