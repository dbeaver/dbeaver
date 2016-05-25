/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

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
                                getEditorInput().getDatabaseObject().getDataSource().getContainer().getPreferenceStore().setValue(
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