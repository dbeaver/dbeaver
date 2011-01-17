/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.utils.ViewUtils;

public class DatabaseNavigatorView extends NavigatorViewBase
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseNavigator";

    public DatabaseNavigatorView()
    {
        super();
    }

    private DBNProject getActiveProjectNode()
    {
        return getModel().getRoot().getProject(DBeaverCore.getInstance().getActiveProject());
    }

    public DBNNode getRootNode() {
        return getActiveProjectNode().getDatabases();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        SashForm sashForm = UIUtils.createPartDivider(this, parent, SWT.VERTICAL | SWT.SMOOTH);
        super.createPartControl(sashForm);

        DatabaseNavigatorTree projectNavigator = createNavigatorTree(sashForm, getActiveProjectNode());
        projectNavigator.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return !(element instanceof DBNProjectDatabases);
            }
        });
    }
}
