/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.qm;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
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

    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);

        queryLogViewer = new QueryLogViewer(group, getSite(), null);
        UIUtils.setHelp(group, IHelpContextIds.CTX_QUERY_MANAGER);
    }

    public void setFocus()
    {
        queryLogViewer.getControl().setFocus();
    }

    public void openFilterDialog()
    {
        PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
            getSite().getShell(),
            this,
            PrefPageQueryManager.PAGE_ID,
            null,//new String[]{pageId},
            null);
        if (propDialog != null) {
            propDialog.open();
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IWorkbenchAdapter.class) {
            return new WorkbenchAdapter() {
                public String getLabel(Object o)
                {
                    return "Query Manager";
                }
            };
        }
        return super.getAdapter(adapter);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
