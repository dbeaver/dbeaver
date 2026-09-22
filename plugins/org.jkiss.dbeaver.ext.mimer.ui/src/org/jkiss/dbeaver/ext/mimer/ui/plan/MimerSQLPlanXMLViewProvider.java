/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.ui.plan;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanSaveProvider;

/**
 * Registers {@link MimerSQLPlanXMLViewer} as an extra "XML" tab on the Explain Execution Plan
 * panel, alongside the standard node-tree tab - see {@code plugin.xml}'s {@code
 * org.jkiss.dbeaver.sql.plan.view} registration (same mechanism {@code ext.cubrid.ui}'s
 * "fulltext" plan view uses).
 *
 * @author Mimer Information Technology
 */
public class MimerSQLPlanXMLViewProvider extends SQLPlanSaveProvider {

    @NotNull
    @Override
    public Viewer createPlanViewer(@NotNull IWorkbenchPart workbenchPart, @NotNull Composite parent) {
        // getControl() must return a *direct* child of parent - the caller uses it as a
        // StackLayout topControl - so the Text widget is created straight in parent, no
        // wrapper composite in between.
        return new MimerSQLPlanXMLViewer(parent);
    }

    @Override
    public void visualizeQueryPlan(@NotNull Viewer viewer, @NotNull SQLQuery query, @NotNull DBCPlan plan) {
        showPlan(viewer, query, plan);
    }

    @Override
    protected void showPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {
        ((MimerSQLPlanXMLViewer) viewer).showPlan(plan);
    }
}
