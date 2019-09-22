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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.task.*;
import org.jkiss.dbeaver.registry.task.TaskRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseTasksView extends ViewPart implements DBTTaskListener {
    public static final String VIEW_ID = "org.jkiss.dbeaver.tasks";

    public static final String CREATE_TASK_CMD_ID = "org.jkiss.dbeaver.task.create";
    public static final String EDIT_TASK_CMD_ID = "org.jkiss.dbeaver.task.edit";
    public static final String RUN_TASK_CMD_ID = "org.jkiss.dbeaver.task.run";

    private Tree taskTree;
    private FilteredTree filteredTree;
    private ViewerColumnController columnController;
    private final List<DBTTask> allTasks = new ArrayList<>();

    //private final SimpleDateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, Locale.ENGLISH);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
    private Color colorError;

    public class NamedObjectPatternFilter extends PatternFilter {
        public NamedObjectPatternFilter() {
            setIncludeLeadingWildcard(true);
        }

        protected boolean isLeafMatch(Viewer viewer, Object element) {
            if (element instanceof DBTTask) {
                return wordMatches(((DBTTask) element).getName());
            }
            return true;
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        ColorRegistry colorRegistry = getSite().getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

        colorError = colorRegistry.get("org.jkiss.dbeaver.txn.color.reverted.background");

        Composite group = UIUtils.createComposite(parent, 1);

        filteredTree = new FilteredTree(group, SWT.MULTI | SWT.FULL_SELECTION, new NamedObjectPatternFilter(), true);
        TreeViewer viewer = filteredTree.getViewer();
        taskTree = viewer.getTree();
        taskTree.setHeaderVisible(true);
        taskTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        columnController = new ViewerColumnController("tasks", filteredTree.getViewer());
        columnController.addColumn("Name", "Task name", SWT.LEFT, true, true, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                DBPImage icon = task.getType().getIcon();
                cell.setImage(DBeaverIcons.getImage(icon != null ? icon : DBIcon.TREE_PACKAGE));
                cell.setText(task.getName());
            }

            @Override
            public String getToolTipText(Object element) {
                String description = ((DBTTask) element).getDescription();
                if (CommonUtils.isEmpty(description)) {
                    description = ((DBTTask) element).getName();
                }
                return description;
            }
        });
        columnController.addColumn("Created", "Task create time", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                cell.setText(dateFormat.format(task.getCreateTime()));
            }
        });
        columnController.addColumn("Last Run", "Task last start time", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    cell.setText("N/A");
                } else {
                    cell.setText(dateFormat.format(lastRun.getStartTime()));
                }
            }
        });
        columnController.addColumn("Last Result", "Task last result", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    cell.setText("N/A");
                } else {
                    if (lastRun.isRunSuccess()) {
                        cell.setText("Success");
                    } else {
                        cell.setText(CommonUtils.notEmpty(lastRun.getErrorMessage()));
                    }
                }
            }
        });
        columnController.addColumn("Last Duration", "Task last run duration", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                DBTTaskRun lastRun = task.getLastRun();
                if (lastRun == null) {
                    cell.setText("N/A");
                } else {
                    cell.setText(RuntimeUtils.formatExecutionTime(lastRun.getRunDuration()));
                }
            }
        });
        columnController.addColumn("Description", "Task description", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                cell.setText(CommonUtils.notEmpty(task.getDescription()));
            }
        });
        columnController.addColumn("Type", "Task type", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                cell.setText(task.getType().getName());
            }
        });
        columnController.addColumn("Category", "Task category", SWT.LEFT, false, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                cell.setText(task.getType().getCategory().getName());
            }
        });
        columnController.addColumn("Project", "Task container project", SWT.LEFT, true, false, new TaskLabelProvider() {
            @Override
            protected void update(ViewerCell cell, DBTTask task) {
                cell.setText(task.getProject().getName());
            }
        });
        columnController.createColumns(true);

        viewer.setContentProvider(new ITreeContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                return ((Collection)inputElement).toArray();
            }

            @Override
            public Object[] getChildren(Object parentElement) {
                return new Object[0];
            }

            @Override
            public Object getParent(Object element) {
                return null;
            }

            @Override
            public boolean hasChildren(Object element) {
                return false;
            }
        });

        MenuManager menuMgr = createContextMenu(viewer);
        getSite().registerContextMenu(menuMgr, viewer);
        getSite().setSelectionProvider(filteredTree.getViewer());

        viewer.addDoubleClickListener(event -> ActionUtils.runCommand(EDIT_TASK_CMD_ID, getSite().getSelectionProvider().getSelection(), getSite()));
        //viewer.addOpenListener(event -> openCurrentTask());

        loadTasks();
    }

    private MenuManager createContextMenu(TreeViewer viewer) {
        final MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> {
            manager.add(ActionUtils.makeCommandContribution(getSite(), RUN_TASK_CMD_ID));
            manager.add(ActionUtils.makeCommandContribution(getSite(), EDIT_TASK_CMD_ID));
//            manager.add(new Action("Open task configuration") {
//                {
//                    setAccelerator(SWT.CR);
//                }
//                @Override
//                public void run() {
//                    openCurrentTask();
//                }
//            });
            manager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.FILE_PROPERTIES, "Task properties", null));
            manager.add(ActionUtils.makeCommandContribution(getSite(), CREATE_TASK_CMD_ID));
            manager.add(ActionUtils.makeCommandContribution(getSite(), IWorkbenchCommandConstants.EDIT_DELETE, "Delete task", null));
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            manager.add(new Separator());
            columnController.fillConfigMenu(manager);
        });

        Control control = viewer.getControl();
        control.setMenu(menuMgr.createContextMenu(control));
        control.addDisposeListener(e -> menuMgr.dispose());
        return menuMgr;
    }

    @Nullable
    private DBTTask getSelectedTask() {
        ISelection selection = filteredTree.getViewer().getSelection();
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
            return null;
        }
        Object element = ((IStructuredSelection) selection).getFirstElement();
        if (!(element instanceof DBTTask)) {
            return null;
        }

        return (DBTTask)element;
    }

    @Override
    public void setFocus() {
        taskTree.setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return adapter.cast(new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o) {
                    return "Database Tasks";
                }
            });
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        TaskRegistry.getInstance().addTaskListener(this);
    }

    @Override
    public void dispose() {
        TaskRegistry.getInstance().removeTaskListener(this);
        super.dispose();
    }

    @Override
    public void handleTaskEvent(DBTTaskEvent event) {
        UIUtils.asyncExec(() -> {
            DBTTask task = event.getTask();
            switch (event.getAction()) {
                case TASK_ADD:
                    allTasks.add(task);
                    filteredTree.getViewer().add(filteredTree.getViewer().getInput(), task);
                    break;
                case TASK_REMOVE:
                    allTasks.remove(task);
                    filteredTree.getViewer().remove(task);
                    break;
                case TASK_UPDATE:
                    filteredTree.getViewer().refresh(task);
                    break;
            }
        });
    }

    private void loadTasks() {
        allTasks.clear();

        List<DBPProject> projectsWithTasks = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            DBTTaskManager taskManager = project.getTaskManager();
            DBTTask[] tasks = taskManager.getTaskConfigurations();
            if (tasks.length == 0) {
                continue;
            }
            projectsWithTasks.add(project);
            Collections.addAll(allTasks, tasks);
        }
        allTasks.sort(Comparator.comparing(DBTTask::getCreateTime));

        filteredTree.getViewer().setInput(allTasks);
        columnController.repackColumns();
    }

    private class TaskLabelProvider extends ColumnLabelProvider {
        @Override
        public final void update(ViewerCell cell) {
            DBTTask task = (DBTTask) cell.getElement();
            DBTTaskRun lastRun = task.getLastRun();
            if (lastRun != null && !lastRun.isRunSuccess()) {
                cell.setBackground(colorError);
            }
            update(cell, task);
        }

        protected void update(ViewerCell cell, DBTTask task) {

        }

        @Override
        public Color getBackground(Object element) {
            return super.getBackground(element);
        }
    }

}
