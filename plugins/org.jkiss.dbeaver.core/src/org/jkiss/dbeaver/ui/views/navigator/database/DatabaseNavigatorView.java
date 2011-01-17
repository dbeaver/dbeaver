/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.database;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.ui.UIUtils;

public class DatabaseNavigatorView extends NavigatorViewBase
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseNavigator";

    private static final String PROP_WEIGHT_TOP = "sash-top";
    private static final String PROP_WEIGHT_BOTTOM = "sash-bottom";

    private int[] weights = new int[] {70, 30};
    private SashForm sashForm;

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
    public void init(IViewSite site, IMemento memento) throws PartInitException
    {
        super.init(site, memento);

        Integer weightTop = memento.getInteger(PROP_WEIGHT_TOP);
        Integer weightBottom = memento.getInteger(PROP_WEIGHT_BOTTOM);
        if (weightTop != null && weightBottom != null) {
            weights = new int[] {weightTop, weightBottom};
        }
    }

    @Override
    public void createPartControl(Composite parent)
    {
        sashForm = UIUtils.createPartDivider(this, parent, SWT.VERTICAL | SWT.SMOOTH);
        super.createPartControl(sashForm);

        DatabaseNavigatorTree projectNavigator = createNavigatorTree(sashForm, getActiveProjectNode());
        projectNavigator.getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return !(element instanceof DBNProjectDatabases);
            }
        });

        sashForm.setWeights(weights);
    }

    @Override
    public void saveState(IMemento memento)
    {
        int[] weights = sashForm.getWeights();
        memento.putInteger(PROP_WEIGHT_TOP, weights[0]);
        memento.putInteger(PROP_WEIGHT_BOTTOM, weights[1]);
        super.saveState(memento);
    }
}
