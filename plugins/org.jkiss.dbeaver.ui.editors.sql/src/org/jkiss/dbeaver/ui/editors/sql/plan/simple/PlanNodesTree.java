/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.plan.simple;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Plan nodes tree
 */
public class PlanNodesTree extends DatabaseObjectListControl<DBCPlanNode> {

    private String query;
    private DBPDataSource dataSource;

    public PlanNodesTree(Composite parent, int style, IWorkbenchSite site)
    {
        super(parent, style, site, CONTENT_PROVIDER);
        setFitWidth(true);
    }

    @Override
    protected ObjectViewerRenderer createRenderer()
    {
        return new PlanTreeRenderer();
    }

    @NotNull
    @Override
    protected String getListConfigId(List<Class<?>> classList) {
        return "ExecutionPlan/" + dataSource.getContainer().getDriver().getId();
    }

    @Override
    protected LoadingJob<Collection<DBCPlanNode>> createLoadService()
    {
        return null;
    }

    public void showPlan(DBPDataSource dataSource, DBCPlan plan) {
        this.dataSource = dataSource;
        List<DBCPlanNode> nodes = new ArrayList<>(plan.getPlanNodes());

        final TreeViewer itemsViewer = (TreeViewer) PlanNodesTree.this.getItemsViewer();
        itemsViewer.getControl().setRedraw(false);
        try {
            clearListData();
            setListData(nodes, false);
            itemsViewer.expandToLevel(10);
        } finally {
            itemsViewer.getControl().setRedraw(true);
        }

    }

    private static ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof DBCPlanNode) {
                Collection<? extends DBCPlanNode> nestedNodes = ((DBCPlanNode) parentElement).getNested();
                return CommonUtils.isEmpty(nestedNodes) ? new Object[0] : nestedNodes.toArray();
            }
            return null;
        }

        @Override
        public Object getParent(Object element)
        {
            if (element instanceof DBCPlanNode) {
                return ((DBCPlanNode)element).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof DBCPlanNode && !CommonUtils.isEmpty(((DBCPlanNode) element).getNested());
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

/*
    public static class ExplainPlanService extends DatabaseLoadService<Collection<DBCPlanNode>> {

        private final DBCQueryPlanner planner;
        private final DBCExecutionContext executionContext;
        private final String query;

        protected ExplainPlanService(DBCQueryPlanner planner, DBCExecutionContext executionContext, String query)
        {
            super("Explain plan", planner.getDataSource());
            this.planner = planner;
            this.executionContext = executionContext;
            this.query = query;
        }

        @Override
        public Collection<DBCPlanNode> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Explain '" + query + "'")) {
                    DBCPlan plan = planner.planQueryExecution(session, query);
                    return (Collection<DBCPlanNode>) plan.getPlanNodes();
                }
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
        }
    }

    public class PlanLoadVisualizer extends ObjectsLoadVisualizer {

        @Override
        public void completeLoading(Collection<DBCPlanNode> items)
        {
            super.completeLoading(items);
            final TreeViewer itemsViewer = (TreeViewer) PlanNodesTree.this.getItemsViewer();
            itemsViewer.getControl().setRedraw(false);
            try {
                itemsViewer.expandToLevel(10);
            } finally {
                itemsViewer.getControl().setRedraw(true);
            }
        }
    }
*/

    private class PlanTreeRenderer extends ViewerRenderer {
        @Override
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }

    }

}
