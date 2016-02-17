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
package org.jkiss.dbeaver.ui.views.qm;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;
import org.jkiss.dbeaver.ui.preferences.PrefPageQueryManager;

public class QueryManagerView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.queryManager";

    private QueryLogViewer queryLogViewer;

    public QueryLogViewer getQueryLogViewer()
    {
        return queryLogViewer;
    }

    @Override
    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);

        queryLogViewer = new QueryLogViewer(group, getSite(), null, true);
        UIUtils.setHelp(group, IHelpContextIds.CTX_QUERY_MANAGER);
    }

    @Override
    public void setFocus()
    {
        queryLogViewer.getControl().setFocus();
    }

    public void openFilterDialog()
    {
        UIUtils.showPreferencesFor(
            getSite().getShell(),
            this,
            PrefPageQueryManager.PAGE_ID);
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IWorkbenchAdapter.class) {
            return new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o)
                {
                    return "Query Manager";
                }
            };
        }
        return super.getAdapter(adapter);
    }
}
