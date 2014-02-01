/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2014 Serge Rieder serge@jkiss.org
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

import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateSQLDialog;

import java.util.Collection;

/**
 * Super class for handling dialogs related to table tools
 * 
 * @author Denis Forveille
 * @author Serge Rieder
 * 
 */
public abstract class DB2TableToolDialog extends GenerateSQLDialog {

    protected final Collection<DB2Table> selectedDB2Tables;

    public DB2TableToolDialog(IWorkbenchPartSite partSite, String title, DB2DataSource db2DataSource,
        Collection<DB2Table> selectedDB2Tables)
    {
        super(partSite, db2DataSource, title, null);
        this.selectedDB2Tables = selectedDB2Tables;
    }

    protected String[] generateSQLScript()
    {
        String[] lines = new String[selectedDB2Tables.size()];
        int index = 0;
        StringBuilder sb = new StringBuilder(512);
        for (DB2Table db2Table : selectedDB2Tables) {
            sb.append("CALL SYSPROC.ADMIN_CMD('");
            sb.append(generateTableCommand(db2Table));
            sb.append("')");
            lines[index++] = sb.toString();
            sb.setLength(0);
        }

        return lines;
    }

    protected abstract StringBuilder generateTableCommand(DB2Table db2Table);

}