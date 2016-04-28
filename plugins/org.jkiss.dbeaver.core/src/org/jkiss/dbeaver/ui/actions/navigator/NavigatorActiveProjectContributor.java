/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.jkiss.dbeaver.Log;
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
    private static final Log log = Log.getLog(NavigatorActiveProjectContributor.class);

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
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DBeaverCore.getInstance().getProjectRegistry().setActiveProject(project);
                }
            });
        }
    }

}