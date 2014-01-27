/*
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
package org.jkiss.dbeaver.ext.mysql.tools.maintenance;

import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateSQLDialog;

import java.util.Collection;

/**
 * Super class for handling dialogs related to table tools
 * 
 * @author Serge Rieder
 * 
 */
public abstract class MySQLTableToolDialog extends GenerateSQLDialog {

    protected final Collection<MySQLTable> selectedTables;

    public MySQLTableToolDialog(IWorkbenchPartSite partSite, String title, MySQLDataSource dataSource,
                                Collection<MySQLTable> selectedTables)
    {
        super(partSite, dataSource, title, null);
        this.selectedTables = selectedTables;
    }

    protected String[] generateSQLScript()
    {
        String[] lines = new String[selectedTables.size()];
        int index = 0;
        StringBuilder sb = new StringBuilder(512);
        for (MySQLTable db2Table : selectedTables) {
//            sb.append("CALL SYSPROC.ADMIN_CMD('");
            generateTableCommand(sb, db2Table);
//            sb.append("')");
            lines[index++] = sb.toString();
            sb.setLength(0);
        }

        return lines;
    }

    protected abstract void generateTableCommand(StringBuilder sql, MySQLTable table);

}