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

        queryLogViewer = new QueryLogViewer(group, getSite(), null, true, false);
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
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == IWorkbenchAdapter.class) {
            return adapter.cast(new WorkbenchAdapter() {
                @Override
                public String getLabel(Object o)
                {
                    return "Query Manager";
                }
            });
        }
        return super.getAdapter(adapter);
    }
}
