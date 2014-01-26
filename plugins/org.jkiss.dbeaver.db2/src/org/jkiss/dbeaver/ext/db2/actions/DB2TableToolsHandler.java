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
import org.jkiss.dbeaver.ext.db2.DB2Messages;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Actions on Tables
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableToolsHandler extends AbstractHandler {

    @Override
    @SuppressWarnings("rawtypes")
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        // Build the list of selected tables
        List<DB2Table> selectedDB2Tables = new ArrayList<DB2Table>();
        IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
        DB2Table db2Table;
        for (Iterator iter = selection.iterator(); iter.hasNext();) {
            db2Table = RuntimeUtils.getObjectAdapter(iter.next(), DB2Table.class);
            if (db2Table != null) {
                selectedDB2Tables.add(db2Table);
            }
        }

        if (!selectedDB2Tables.isEmpty()) {

            // Pick the datasource from one of the tables, let's hope they are all coming from the same DS..
            DB2DataSource dataSource = selectedDB2Tables.get(0).getDataSource();
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

            Command command = null;
            try {
                command = Command.getCommandFromId(event.getCommand().getId());
            } catch (IllegalAccessException e) {
                throw new ExecutionException(e.getMessage());
            }
            switch (command) {
            case REORG:
                performReorg(activePart.getSite(), dataSource, selectedDB2Tables);
                break;
            case REORGIX:
                performReorgIx(activePart.getSite(), dataSource, selectedDB2Tables);
                break;
            case RUNSTATS:
                performRunstats(activePart.getSite(), dataSource, selectedDB2Tables);
                break;
            case TRUNCATE:
                performTruncate(activePart.getSite(), dataSource, selectedDB2Tables);
                break;
            }
        }

        return null;
    }

    // -------
    // Helpers
    // -------
    private void performReorg(final IWorkbenchPartSite partSite, DB2DataSource dataSource,
        final Collection<DB2Table> selectedDB2Tables)
    {

        // Reorg Options can not be set for all tables at once due to index selection
        for (DB2Table db2Table : selectedDB2Tables) {
            DB2TableReorgDialog dialog = new DB2TableReorgDialog(partSite, dataSource, db2Table);
            dialog.open();
            dialog.setOnSuccess(new Runnable() {
                @Override
                public void run()
                {
                    UIUtils.showMessageBox(partSite.getShell(), DB2Messages.dialog_table_tools_success_title,
                        DB2Messages.dialog_table_tools_reorg_success, SWT.ICON_INFORMATION);
                }
            });
        }
    }

    private void performReorgIx(final IWorkbenchPartSite partSite, DB2DataSource dataSource,
        final Collection<DB2Table> selectedDB2Tables)
    {
        DB2TableReorgIndexDialog dialog = new DB2TableReorgIndexDialog(partSite, dataSource, selectedDB2Tables);
        dialog.open();
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), DB2Messages.dialog_table_tools_success_title,
                    DB2Messages.dialog_table_tools_reorgix_success, SWT.ICON_INFORMATION);
            }
        });
    }

    private void performRunstats(final IWorkbenchPartSite partSite, DB2DataSource dataSource,
        final Collection<DB2Table> selectedDB2Tables)
    {
        DB2TableRunstatsDialog dialog = new DB2TableRunstatsDialog(partSite, dataSource, selectedDB2Tables);
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), DB2Messages.dialog_table_tools_success_title,
                    DB2Messages.dialog_table_tools_runstats_success, SWT.ICON_INFORMATION);
            }
        });
        dialog.open();
    }

    private void performTruncate(final IWorkbenchPartSite partSite, DB2DataSource dataSource,
        final Collection<DB2Table> selectedDB2Tables)
    {
        DB2TableTruncateDialog dialog = new DB2TableTruncateDialog(partSite, dataSource, selectedDB2Tables);
        dialog.setOnSuccess(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(partSite.getShell(), DB2Messages.dialog_table_tools_success_title,
                    DB2Messages.dialog_table_tools_truncate_success, SWT.ICON_INFORMATION);
            }
        });
        dialog.open();
    }

    // -------
    // Helpers
    // -------

    private enum Command {
        REORG("org.jkiss.dbeaver.ext.db2.table.reorg"),

        REORGIX("org.jkiss.dbeaver.ext.db2.table.reorgix"),

        RUNSTATS("org.jkiss.dbeaver.ext.db2.table.runstats"),

        TRUNCATE("org.jkiss.dbeaver.ext.db2.table.truncate");

        private String commandId;

        // -----------
        // Constructor
        // -----------

        private Command(String commandId)
        {
            this.commandId = commandId;
        }

        // -------
        // Helpers
        // -------
        public static Command getCommandFromId(String commandId) throws IllegalAccessException
        {
            for (Command command : Command.values()) {
                if (command.commandId.equals(commandId)) {
                    return command;
                }
            }
            throw new IllegalAccessException(commandId + " is not a valid command Id");
        }
    }

}