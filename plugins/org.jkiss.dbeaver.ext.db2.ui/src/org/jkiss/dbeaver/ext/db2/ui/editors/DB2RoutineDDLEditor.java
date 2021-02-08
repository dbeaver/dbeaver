/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.ui.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2DDLFormat;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * Manage the "Format" drop down box in the bottom right corner of the Routine DDL viewer toformat DDL
 * 
 * @author Denis Forveille
 * 
 */
public class DB2RoutineDDLEditor extends SQLSourceViewer<DB2Routine> {

    public DB2RoutineDDLEditor()
    {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);

        contributionManager.add(new Separator());
        contributionManager.add(new ControlContribution("DDLFormat") {
            @Override
            protected Control createControl(Composite parent)
            {
                DB2DDLFormat ddlFormat = DB2DDLFormat.getCurrentFormat(getSourceObject().getDataSource());
                final Combo ddlFormatCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
                ddlFormatCombo.setToolTipText("DDL Format");
                for (DB2DDLFormat format : DB2DDLFormat.values()) {
                    ddlFormatCombo.add(format.getTitle());
                    if (format == ddlFormat) {
                        ddlFormatCombo.select(ddlFormatCombo.getItemCount() - 1);
                    }
                }

                ddlFormatCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        for (DB2DDLFormat format : DB2DDLFormat.values()) {
                            if (format.ordinal() == ddlFormatCombo.getSelectionIndex()) {
                                getSourceObject().getDataSource().getContainer().getPreferenceStore()
                                    .setValue(DB2Constants.PREF_KEY_DDL_FORMAT, format.name());
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