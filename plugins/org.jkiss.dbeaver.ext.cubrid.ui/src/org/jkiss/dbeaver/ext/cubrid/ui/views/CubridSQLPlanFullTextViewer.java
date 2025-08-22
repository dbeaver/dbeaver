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
package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.UIUtils;

public class CubridSQLPlanFullTextViewer extends Viewer {

    private SashForm textPanel;
    private Text sqlText;
    private Text fullText;

    public CubridSQLPlanFullTextViewer(IWorkbenchPart workbenchPart, Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        textPanel = UIUtils.createPartDivider(workbenchPart, composite, SWT.VERTICAL);
        textPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        fullText = createReadOnlyText(textPanel);
        sqlText = createReadOnlyText(textPanel);

        textPanel.setWeights(new int[] {80, 20});
    }

    private Text createReadOnlyText(Composite parent) {
        Text text = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        return text;
    }

    public void showPlan(SQLQuery query, DBCPlan plan) {
        this.sqlText.setText(plan.getQueryString());
        this.fullText.setText(query.getText());
    }

    @Override
    public Control getControl() {
        return textPanel.getParent();
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

    }

    @Override
    public void setInput(Object input) {

    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {

    }

}
