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
package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.task.DBTScheduler;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskFolder;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.CommonUtils;

/**
 * TaskPropertyTester
 */
public class TaskPropertyTester extends PropertyTester
{
    private static final Log log = Log.getLog(TaskPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.task";
    public static final String PROP_SCHEDULED = "scheduled";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_CAN_OPEN_SCHEDULER_SETTINGS = "canOpenSchedulerSettings";

    public TaskPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof DBTTaskFolder && PROP_EDITABLE.equals(property)) {
            return ((DBTTaskFolder) receiver).getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
        }
        if (!(receiver instanceof DBTTask task)) {
            return false;
        }
        final DBTScheduler scheduler = TaskRegistry.getInstance().getActiveSchedulerInstance();
        final boolean actualValue = switch (property) {
            case PROP_SCHEDULED -> scheduler != null && scheduler.getScheduledTaskInfo(task) != null;
            case PROP_EDITABLE -> task.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
            case PROP_CAN_OPEN_SCHEDULER_SETTINGS -> scheduler != null && scheduler.supportsFeature(DBTScheduler.FEATURE_OPEN_EXTERNAL_SETTINGS);
            default -> false;
        };
        return actualValue == CommonUtils.getBoolean(expectedValue, true);
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}