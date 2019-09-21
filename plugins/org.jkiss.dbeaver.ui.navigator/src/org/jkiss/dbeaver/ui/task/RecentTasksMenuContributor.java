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
package org.jkiss.dbeaver.ui.task;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskRun;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceMenuContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecentTasksMenuContributor extends DataSourceMenuContributor
{
    private static final Log log = Log.getLog(RecentTasksMenuContributor.class);

    private static final int MAX_ITEMS = 5;

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        DBPProject project = NavigatorUtils.getSelectedProject();
        if (project == null) {
            return;
        }
        DBTTask[] tasks = project.getTaskManager().getTaskConfigurations();
        Arrays.sort(tasks, (o1, o2) -> {
            DBTTaskRun lr1 = o1.getLastRun();
            DBTTaskRun lr2 = o1.getLastRun();
            if (lr1 == lr2) return o1.getCreateTime().compareTo(o2.getCreateTime());
            else if (lr1 == null) return -1;
            else if (lr2 == null) return 1;
            else return lr1.getStartTime().compareTo(lr2.getStartTime());
        });

        for (int i = 0; i < tasks.length && i <= MAX_ITEMS; i++) {
            menuItems.add(ActionUtils.makeCommandContribution(
                UIUtils.getActiveWorkbenchWindow(),
                DatabaseTasksView.RUN_TASK_CMD_ID,
                SWT.PUSH,
                null,
                null,
                null,
                false,
                Collections.singletonMap("task", tasks[i].getId())));
        }
    }

}