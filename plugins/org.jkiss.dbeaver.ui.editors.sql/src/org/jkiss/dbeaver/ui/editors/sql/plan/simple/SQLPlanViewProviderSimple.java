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
package org.jkiss.dbeaver.ui.editors.sql.plan.simple;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanSaveProvider;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanViewProvider;

/**
 * SQLPlanViewProviderSimple
 */
public class SQLPlanViewProviderSimple extends SQLPlanSaveProvider {


    @Override
    public Viewer createPlanViewer(IWorkbenchPart workbenchPart, Composite parent) {
        SQLPlanTreeViewer treeViewer = new SQLPlanTreeViewer(workbenchPart, parent);
        return treeViewer;
    }

    @Override
    public void visualizeQueryPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {        
        fillPlan(query,plan);
        showPlan(viewer,query, plan);
    }

    @Override
    public void contributeActions(Viewer viewer, IContributionManager contributionManager, SQLQuery lastQuery, DBCPlan lastPlan) {
        super.contributeActions(viewer, contributionManager, lastQuery, lastPlan);

        SQLPlanTreeViewer treeViewer = (SQLPlanTreeViewer) viewer;
        treeViewer.contributeActions(contributionManager, lastQuery, lastPlan);
    }

    @Override
    protected void showPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {
        SQLPlanTreeViewer treeViewer = (SQLPlanTreeViewer) viewer;
        treeViewer.showPlan(query, plan);      
    }

}
