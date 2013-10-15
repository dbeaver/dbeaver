/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateSQLDialog;

import java.util.Collection;

/**
 * Manage the Dialog to enter Reorg Table Index option
 * 
 * @author Denis Forveille
 * 
 */
public abstract class DB2TableToolDialog extends GenerateSQLDialog {

    protected final Collection<DB2Table> tables;

    public DB2TableToolDialog(IWorkbenchPartSite partSite, String title, DB2DataSource dataSource, Collection<DB2Table> tables)
    {
        super(partSite, dataSource, title, null);
        this.tables = tables;
    }

    protected class SQLChangeListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e)
        {
            updateSQL();
        }
    }

    protected String[] generateSQLScript()
    {
        String[] lines = new String[tables.size()];
        int index = 0;
        for (DB2Table db2Table : tables) {
            StringBuilder sb = new StringBuilder(512);
            sb.append("CALL SYSPROC.ADMIN_CMD('");

            sb.append(generateTableCommand(db2Table));

            sb.append("')");
            lines[index++] = sb.toString();
        }

        return lines;
    }

    protected abstract String generateTableCommand(DB2Table db2Table);

}