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
package org.jkiss.dbeaver.ui.navigator.project;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.NavigatorStatePersistor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.project.PrefPageProjectResourceSettings;

/**
 * ProjectNavigatorView
 */
public class ProjectNavigatorView extends DecoratedProjectView
{

    private static final Log log = Log.getLog(ProjectNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectNavigator";

    private IMemento memento;

    public ProjectNavigatorView() {
    }

    @Override
    public void saveState(IMemento memento) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH) > 0)
            new NavigatorStatePersistor().saveState(getNavigatorViewer().getExpandedElements(), memento);
    }

    private void restoreState() {
        int maxDepth = DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_RESTORE_STATE_DEPTH);
        if (maxDepth > 0)
            new NavigatorStatePersistor().restoreState(getNavigatorViewer(), getRootNode(), maxDepth, memento);
    }

    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException
    {
        this.memento = memento;
        super.init(site, memento);
    }

    @Override
    public DBNNode getRootNode()
    {
        return getModel().getRoot();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);

        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_NAVIGATOR);
        UIExecutionQueue.queueExec(this::restoreState);

        getNavigatorTree().setLabelDecorator(labelDecorator);
    }

    @Override
    public void configureView() {
        DBPProject project = NavigatorUtils.getSelectedProject();
        if (project != null) {
            UIUtils.showPreferencesFor(getSite().getShell(), project.getEclipseProject(), PrefPageProjectResourceSettings.PAGE_ID);
        } else {
            ActionUtils.runCommand(IWorkbenchCommandConstants.WINDOW_PREFERENCES, UIUtils.getActiveWorkbenchWindow());
        }

    }
}
