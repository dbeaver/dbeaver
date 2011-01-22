/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.jkiss.dbeaver.core.DBeaverCore;

public class NavigatorActiveProjectContributor extends ContributionItem
{
    static final Log log = LogFactory.getLog(NavigatorActiveProjectContributor.class);

    @Override
    public void fill(Menu menu, int index)
    {
        createMenu(menu);
    }
    
    private void createMenu(final Menu menu)
    {
        final IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        for (final IProject project : DBeaverCore.getInstance().getLiveProjects()) {
            MenuItem txnItem = new MenuItem(menu, SWT.RADIO);
            txnItem.setText(project.getName());
            txnItem.setSelection(project == activeProject);
            txnItem.setData(project);
            txnItem.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    DBeaverCore.getInstance().getProjectRegistry().setActiveProject(project);
                }
            });
        }
    }

}