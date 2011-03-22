/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.plan;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Plan nodes tree
 */
public class PlanNodesTree extends ObjectListControl<DBCPlanNode> {

    private static ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof DBCPlanNode) {
                Collection<? extends DBCPlanNode> nestedNodes = ((DBCPlanNode) parentElement).getNested();
                return CommonUtils.isEmpty(nestedNodes) ? new Object[0] : nestedNodes.toArray();
            }
            return null;
        }

        public Object getParent(Object element)
        {
            if (element instanceof DBCPlanNode) {
                return ((DBCPlanNode)element).getParent();
            }
            return null;
        }

        public boolean hasChildren(Object element)
        {
            return element instanceof DBCPlanNode && !CommonUtils.isEmpty(((DBCPlanNode) element).getNested());
        }

        public void dispose()
        {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    private IDataSourceProvider dataSourceProvider;
    private IWorkbenchPart workbenchPart;

    public PlanNodesTree(Composite parent, int style, IWorkbenchPart workbenchPart, IDataSourceProvider dataSourceProvider)
    {
        super(parent, style);
        this.dataSourceProvider = dataSourceProvider;
        this.workbenchPart = workbenchPart;
        setFitWidth(true);

        createContextMenu();
    }

    protected DBPDataSource getDataSource()
    {
        return dataSourceProvider.getDataSource();
    }

    @Override
    protected IContentProvider createContentProvider()
    {
        return CONTENT_PROVIDER;
    }

    @Override
    public void clearData()
    {
        super.clearData();
        //createColumn("", "", null);
    }

    public void fillData(DBCQueryPlanner planner, String query)
    {
        super.loadData(
            LoadingUtils.createService(
                new ExplainPlanService(planner, query),
                new ObjectsLoadVisualizer()));
    }

    private void createContextMenu()
    {
        Control control = getItemsViewer().getControl();
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action("Copy") {
                    public void run()
                    {
                        String text = getSelectedText();
                        if (text != null) {
                            TextTransfer textTransfer = TextTransfer.getInstance();
                            Clipboard clipboard = new Clipboard(getDisplay());
                            clipboard.setContents(
                                new Object[]{text},
                                new Transfer[]{textTransfer});
                        }
                    }
                };
                copyAction.setEnabled(!getItemsViewer().getSelection().isEmpty());
                //copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                manager.add(copyAction);

                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
        workbenchPart.getSite().registerContextMenu(menuMgr, getItemsViewer());
    }


    private class ExplainPlanService extends DatabaseLoadService<Collection<DBCPlanNode>> {

        private DBCQueryPlanner planner;
        private String query;

        protected ExplainPlanService(DBCQueryPlanner planner, String query)
        {
            super("Explain plan", getDataSource());
            this.planner = planner;
            this.query = query;
        }

        public Collection<DBCPlanNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                if (getDataSource() == null) {
                    throw new DBCException("No database connection");
                }

                DBCExecutionContext context = getDataSource().openContext(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Explain '" + query + "'");
                try {
                    DBCPlan plan = planner.planQueryExecution(context, query);
                    return plan.getPlanNodes();
                }
                finally {
                    context.close();
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

}
