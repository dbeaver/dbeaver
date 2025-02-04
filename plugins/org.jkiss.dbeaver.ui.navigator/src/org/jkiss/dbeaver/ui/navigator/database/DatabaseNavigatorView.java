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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.navigator.DBNEmptyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorStatePersister;

public class DatabaseNavigatorView extends NavigatorViewBase implements DBPProjectListener {
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseNavigator";
    private IMemento memento;

    public DatabaseNavigatorView()
    {
        super();
        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(this);
    }

    @Override
    public void saveState(IMemento memento) {
        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        if (preferences.getInt(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH) > 0) {
            NavigatorStatePersister.saveExpandedState(getNavigatorViewer().getExpandedElements(), memento);
        }
        if (preferences.getBoolean(NavigatorPreferences.NAVIGATOR_RESTORE_FILTER)) {
            NavigatorStatePersister.saveFilterState(getNavigatorTree(), memento);
        }
    }

    private void restoreState() {
        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        final int maxDepth = preferences.getInt(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH);
        if (maxDepth > 0) {
            NavigatorStatePersister.restoreExpandedState(getNavigatorViewer(), getRootNode(), maxDepth, memento);
        }
        if (preferences.getBoolean(NavigatorPreferences.NAVIGATOR_RESTORE_FILTER)) {
            NavigatorStatePersister.restoreFilterState(getNavigatorTree(), memento);
        }
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException
    {
        this.memento = memento;
        super.init(site, memento);
    }

    @Override
    protected INavigatorFilter getNavigatorFilter() {
        return new DatabaseNavigatorTreeFilter();
    }

    @Override
    public DBNNode getRootNode()
    {
        DBNProject projectNode = getGlobalNavigatorModel().getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        return projectNode == null ? new DBNEmptyNode() : projectNode.getDatabases();
    }

    @Override
    public void dispose()
    {
        DBPPlatformDesktop.getInstance().getWorkspace().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        UIUtils.setHelp(parent, IHelpContextIds.CTX_DATABASE_NAVIGATOR);
        UIExecutionQueue.queueExec(this::restoreState);
    }

    @Override
    protected void createTreeColumns(DatabaseNavigatorTree tree) {
/*
        Tree treeControl = tree.getViewer().getTree();

        final TreeViewerColumn nameColumn = new TreeViewerColumn(tree.getViewer(), SWT.LEFT);
        nameColumn.setLabelProvider((CellLabelProvider) tree.getViewer().getLabelProvider());
        final TreeViewerColumn statColumn = new TreeViewerColumn(tree.getViewer(), SWT.RIGHT);
        statColumn.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {

            }
        });
        treeControl.addListener(SWT.Resize, event -> {
            int treeWidth = treeControl.getSize().x - treeControl.getVerticalBar().getSize().x - treeControl.getBorderWidth() * 2;
            nameColumn.getColumn().setWidth(treeWidth * 80 / 100);
            statColumn.getColumn().setWidth(treeWidth * 20 / 100);
        });
*/
    }

    @Override
    public void handleProjectAdd(@NotNull DBPProject project) {
        // Ignore
    }

    @Override
    public void handleProjectRemove(@NotNull DBPProject project) {
        // Ignore
    }

    @Override
    public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue)
    {
        UIExecutionQueue.queueExec(() -> {
            getNavigatorTree().getViewer().setInput(new DatabaseNavigatorContent(getRootNode()));
            getSite().getSelectionProvider().setSelection(new StructuredSelection());
        });

    }

}
