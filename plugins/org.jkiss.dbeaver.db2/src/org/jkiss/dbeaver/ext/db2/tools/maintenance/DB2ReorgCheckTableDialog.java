/*
 * Copyright (C) 2013-2014 Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.tools.maintenance;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;

import java.util.Collection;
import java.util.List;

/**
 * DB2 Table Reorg Check dialog
 */
public class DB2ReorgCheckTableDialog extends DB2BaseTableToolDialog {

    private static final int NB_RESULT_COLS = 12;

    public DB2ReorgCheckTableDialog(IWorkbenchPartSite partSite, final Collection<DB2Table> selectedTables)
    {
        super(partSite, DB2Messages.dialog_table_tools_reorgcheck_title, selectedTables);
    }

    @Override
    protected int getNumberExtraResultingColumns()
    {
        return NB_RESULT_COLS;
    }

    @Override
    protected void createControls(Composite parent)
    {
        // Object Selector
        createObjectsSelector(parent);
    }

    @Override
    protected void generateObjectCommand(List<String> lines, DB2Table db2Table)
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("CALL SYSPROC.REORGCHK_TB_STATS('T','");
        sb.append(db2Table.getFullQualifiedName());
        sb.append("')");

        lines.add(sb.toString());
    }
}
