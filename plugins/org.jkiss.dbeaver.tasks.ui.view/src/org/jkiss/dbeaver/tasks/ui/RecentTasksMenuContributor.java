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
package org.jkiss.dbeaver.tasks.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.ui.view.TaskHandlerRun;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceMenuContributor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RecentTasksMenuContributor extends DataSourceMenuContributor
{
    private static final int MAX_ITEMS = 5;

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems)
    {
        DBPProject project = NavigatorUtils.getSelectedProject();
        if (project == null) {
            return;
        }
        DBTTask[] tasks = Arrays.stream(project.getTaskManager().getAllTasks())
            .sorted(Comparator.comparing(DBTTask::getUpdateTime).reversed())
            .limit(MAX_ITEMS)
            .toArray(DBTTask[]::new);

        for (DBTTask task : tasks) {
            DBPImage taskIcon = task.getType().getIcon();
            if (taskIcon == null) taskIcon = DBIcon.TREE_TASK;
            menuItems.add(ActionUtils.makeActionContribution(new Action(task.getName(), DBeaverIcons.getImageDescriptor(taskIcon)) {
                @Override
                public void run() {
                    TaskHandlerRun.runTask(task);
                }
            }, false));
        }
    }

}