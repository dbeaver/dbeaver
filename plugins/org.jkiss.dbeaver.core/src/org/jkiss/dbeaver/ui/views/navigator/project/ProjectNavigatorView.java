/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.project;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;

/**
 * ProjectNavigatorView
 */
public class ProjectNavigatorView extends NavigatorViewBase // CommonNavigator
{

    static final Log log = Log.getLog(ProjectNavigatorView.class);

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
