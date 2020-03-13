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

package org.jkiss.dbeaver.ext.oracle.ui.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDDLFormat;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * OracleObjectDDLEditor
 */
public class OracleObjectDDLEditor extends SQLSourceViewer<OracleTable> {

    public OracleObjectDDLEditor()
    {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);

        contributionManager.add(new Separator());
        contributionManager.add(new ControlContribution("DDLFormat") {
            @Override
            protected Control createControl(Composite parent) {
                OracleDDLFormat ddlFormat = OracleDDLFormat.getCurrentFormat(getSourceObject().getDataSource());
                final Combo ddlFormatCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
                ddlFormatCombo.setToolTipText("DDL Format");
                for (OracleDDLFormat format : OracleDDLFormat.values()) {
                    ddlFormatCombo.add(format.getTitle());
                    if (format == ddlFormat) {
                        ddlFormatCombo.select(ddlFormatCombo.getItemCount() - 1);
                    }
                }
                ddlFormatCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        for (OracleDDLFormat format : OracleDDLFormat.values()) {
                            if (format.ordinal() == ddlFormatCombo.getSelectionIndex()) {
                                getSourceObject().getDataSource().getContainer().getPreferenceStore().setValue(
                                    OracleConstants.PREF_KEY_DDL_FORMAT, format.name());
                                refreshPart(this, true);
                                break;
                            }
                        }
                    }
                });
                return ddlFormatCombo;
            }
        });
    }

}