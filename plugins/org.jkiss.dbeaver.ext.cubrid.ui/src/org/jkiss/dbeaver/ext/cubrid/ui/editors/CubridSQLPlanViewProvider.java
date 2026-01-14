/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.cubrid.ui.editors;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ext.cubrid.model.plan.CubridPlanAnalyser;
import org.jkiss.dbeaver.ext.cubrid.ui.views.CubridSQLPlanFullTextViewer;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanSaveProvider;

public class CubridSQLPlanViewProvider extends SQLPlanSaveProvider {

    @Override
    public Viewer createPlanViewer(IWorkbenchPart workbenchPart, Composite parent) {
        CubridSQLPlanFullTextViewer treeViewer = new CubridSQLPlanFullTextViewer(workbenchPart, parent);
        return treeViewer;
    }

    @Override
    public void visualizeQueryPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {
        SQLQuery fullText = new SQLQuery(query.getDataSource(), ((CubridPlanAnalyser) plan).getPlanQueryString());
        showPlan(viewer, fullText, plan);
    }

    @Override
    protected void showPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {
        CubridSQLPlanFullTextViewer fullText = (CubridSQLPlanFullTextViewer) viewer;
        fullText.showPlan(query, plan);
    }

}
