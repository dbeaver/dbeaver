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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Plain read-only view of {@link DBCPlan#getPlanSourceData()} - the raw {@code
 * getExplainText()} XML, unparsed. A fallback for whatever {@link
 * org.jkiss.dbeaver.ext.mimer.model.plan.MimerPlanNode} doesn't (yet, or ever) map onto a
 * tree row.
 *
 * @author Mimer Information Technology
 */
public class MimerSQLPlanXMLViewer extends Viewer {

    private final Text text;

    public MimerSQLPlanXMLViewer(@NotNull Composite parent) {
        text = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.setFont(UIUtils.getMonospaceFont());
    }

    public void showPlan(@NotNull DBCPlan plan) {
        Object sourceData = plan.getPlanSourceData();
        text.setText(sourceData == null ? "" : CommonUtils.toString(sourceData));
    }

    @Override
    public Control getControl() {
        return text;
    }

    @Override
    public Object getInput() {
        return null;
    }

    @Override
    public ISelection getSelection() {
        return null;
    }

    @Override
    public void refresh() {
        // no-op - content is set explicitly by showPlan()
    }

    @Override
    public void setInput(Object input) {
        // no-op - content is set explicitly by showPlan()
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        // no-op
    }
}
