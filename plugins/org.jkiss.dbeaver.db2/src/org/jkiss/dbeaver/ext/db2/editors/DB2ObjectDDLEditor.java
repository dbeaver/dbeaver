/*
 * Copyright (C) 2010-2013 Serge Rieder
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

package org.jkiss.dbeaver.ext.db2.editors;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;

/**
 * DB2ObjectDDLEditor
 */
public class DB2ObjectDDLEditor extends SQLEditorNested<DB2Table> {

    public DB2ObjectDDLEditor()
    {
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        super.init(site, input);
    }

    @Override
    public boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        String ddlFormatString = getEditorInput().getDatabaseObject().getDataSource().getContainer().getPreferenceStore()
            .getString(DB2Constants.PREF_KEY_DDL_FORMAT);
        return ((DB2Table) getEditorInput().getDatabaseObject()).getDDL(monitor);
    }

    @Override
    protected void setSourceText(String sourceText)
    {
    }

    @Override
    protected void contributeEditorCommands(ToolBarManager toolBarManager)
    {
        super.contributeEditorCommands(toolBarManager);
    }
}