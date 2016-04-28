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
package org.jkiss.dbeaver.ui.navigator.project;

import org.jkiss.dbeaver.Log;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;

/**
 * ProjectNavigatorView
 */
public class ProjectNavigatorView extends NavigatorViewBase // CommonNavigator
{

    private static final Log log = Log.getLog(ProjectNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectNavigator";

    public ProjectNavigatorView() {
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
    }
}
