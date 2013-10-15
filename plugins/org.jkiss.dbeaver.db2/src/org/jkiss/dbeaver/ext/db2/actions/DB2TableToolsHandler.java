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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * TODO DF: Work In Progress !!!!
 * 
 * Actions on Tables
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableToolsHandler extends AbstractHandler {

    private static final String CMD_REORG_ID = "org.jkiss.dbeaver.ext.db2.table.reorg";
    private static final String CMD_REORGIX_ID = "org.jkiss.dbeaver.ext.db2.table.reorgix";
    private static final String CMD_RUNSTATS_ID = "org.jkiss.dbeaver.ext.db2.table.runstats";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        List<DB2Table> tables = new ArrayList<DB2Table>();
        DB2DataSource dataSource = null;
        for (Iterator iter = selection.iterator(); iter.hasNext(); ) {
            final DB2Table db2Table = RuntimeUtils.getObjectAdapter(iter.next(), DB2Table.class);
            if (db2Table != null) {
                tables.add(db2Table);
                dataSource = db2Table.getDataSource();
            }
        }

        if (!tables.isEmpty()) {
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

            if (event.getCommand().getId().equals(CMD_REORG_ID)) {
                performReorg(activePart.getSite(), dataSource, tables);
            } else if (event.getCommand().getId().equals(CMD_REORGIX_ID)) {
                performReorgIx(activePart.getSite(), dataSource, tables);
            } else if (event.getCommand().getId().equals(CMD_RUNSTATS_ID)) {
                performRunstats(activePart.getSite(), dataSource, tables);
            }
        }

        return null;
    }

    // -------
    // Helpers
    // -------
    private void performReorg(final IWorkbenchPartSite partSite, DB2DataSource dataSource, final Collection<DB2Table> tables)
    {

        DB2TableReorgDialog2 dialog = new DB2TableReorgDialog2(partSite, dataSource, tables.iterator().next());
        dialog.open();
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), "OK", "Reorg finished", SWT.ICON_INFORMATION);
            }
        });
    }

    private void performReorgIx(final IWorkbenchPartSite partSite, DB2DataSource dataSource, final Collection<DB2Table> tables)
    {
        DB2TableReorgIndexDialog2 dialog = new DB2TableReorgIndexDialog2(partSite, dataSource, tables);
        dialog.open();
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), "OK", "ReorgIx finished", SWT.ICON_INFORMATION);
            }
        });
    }

    private void performRunstats(final IWorkbenchPartSite partSite, DB2DataSource dataSource, final Collection<DB2Table> tables)
    {
        DB2TableRunstatsDialog2 dialog = new DB2TableRunstatsDialog2(partSite, dataSource, tables);
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), "OK", "Runstats finished", SWT.ICON_INFORMATION);
            }
        });
        dialog.open();
    }

}