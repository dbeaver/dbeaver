/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.itemlist.ObjectListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Session table
 */
class SessionTable extends ObjectListControl<DBAServerSession> implements IDataSourceProvider {

    private IDataSourceProvider dataSourceProvider;
    private IWorkbenchPart workbenchPart;

    private DBAServerSessionManager planner;
    private String query;

    public SessionTable(Composite parent, int style, IWorkbenchPart workbenchPart, IDataSourceProvider dataSourceProvider)
    {
        super(parent, style, CONTENT_PROVIDER);
        this.dataSourceProvider = dataSourceProvider;
        this.workbenchPart = workbenchPart;
        setFitWidth(true);

        createContextMenu();
    }

    public DBPDataSource getDataSource()
    {
        return dataSourceProvider.getDataSource();
    }

    protected ObjectViewerRenderer createRenderer()
    {
        return new PlanTreeRenderer();
    }

    @Override
    protected LoadingJob<Collection<DBAServerSession>> createLoadService()
    {
        return LoadingUtils.createService(
                new ExplainPlanService(),
                new ObjectsLoadVisualizer());
    }

    private void createContextMenu()
    {
        Control control = getControl();
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(control);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action("Copy") {
                    public void run()
                    {
                        String text = getRenderer().getSelectedText();
                        if (text != null) {
                            TextTransfer textTransfer = TextTransfer.getInstance();
                            Clipboard clipboard = new Clipboard(getDisplay());
                            clipboard.setContents(
                                new Object[]{text},
                                new Transfer[]{textTransfer});
                        }
                    }
                };
                copyAction.setEnabled(!getSelectionProvider().getSelection().isEmpty());
                manager.add(copyAction);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        control.setMenu(menu);
        workbenchPart.getSite().registerContextMenu(menuMgr, getSelectionProvider());
    }

    public void init(DBAServerSessionManager planner, String query)
    {
        this.planner = planner;
        this.query = query;
    }

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

    private class ExplainPlanService extends DatabaseLoadService<Collection<DBAServerSession>> {

        protected ExplainPlanService()
        {
            super("Explain plan", getDataSource());
        }

        public Collection<DBAServerSession> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                if (getDataSource() == null) {
                    throw new DBCException("No database connection");
                }

                DBCExecutionContext context = getDataSource().openContext(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Explain '" + query + "'");
                try {
                    return planner.getSessions(context, null);
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

    private class PlanTreeRenderer extends ViewerRenderer {
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }
    }

}
