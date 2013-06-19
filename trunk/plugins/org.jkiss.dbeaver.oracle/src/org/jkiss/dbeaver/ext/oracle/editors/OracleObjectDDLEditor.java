/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;

/**
 * OracleObjectDDLEditor
 */
public class OracleObjectDDLEditor extends SQLEditorNested<OracleTable> {

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return ((OracleTable)getEditorInput().getDatabaseObject()).getDDL(monitor);
    }

    @Override
    protected void setSourceText(String sourceText) {
    }

    @Override
    protected void contributeEditorCommands(ToolBarManager toolBarManager)
    {
        super.contributeEditorCommands(toolBarManager);
        toolBarManager.add(new Separator());
        toolBarManager.add(new ControlContribution("DDLFormat")
        {
            @Override
            protected Control createControl(Composite parent)
            {
                CCombo ddlFormatCombo = new CCombo(parent, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
                ddlFormatCombo.setToolTipText("DDL Format");
                ddlFormatCombo.add("Full DDL");
                ddlFormatCombo.add("No storage information");
                ddlFormatCombo.add("Compact form");
                return ddlFormatCombo;
            }
        });
    }
}