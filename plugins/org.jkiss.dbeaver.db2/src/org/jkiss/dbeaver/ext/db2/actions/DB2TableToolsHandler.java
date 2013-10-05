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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Add Menu with command on DB2Tables
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableToolsHandler extends CompoundContributionItem {

    private static final Log LOG = LogFactory.getLog(DB2TableToolsHandler.class);

    private static final String SQL_REORG = "CALL SYSPROC.ADMIN_CMD('REORG TABLE %s');\n";
    private static final String SQL_RUNSTATS = "CALL SYSPROC.ADMIN_CMD('RUNSTATS ON TABLE %s WITH DISTRIBUTION AND DETAILED INDEXES ALL');\n";
    private static final String SQL_DESCRIBE = "CALL SYSPROC.ADMIN_CMD('DESCRIBE TABLE %s SHOW DETAIL');\n";
    private static final String SQL_INTEGRITY = " SET INTEGRITY FOR %s IMMEDIATE CHECKED;')\n";

    @Override
    protected IContributionItem[] getContributionItems()
    {
        IWorkbenchPart part = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart();
        IStructuredSelection structuredSelection = DB2TableToolsHandler.getSelectionFromPart(part);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return new IContributionItem[0];
        }

        final DBSTable table = (DBSTable) ((DBNDatabaseNode) RuntimeUtils.getObjectAdapter(structuredSelection.getFirstElement(),
            DBNNode.class)).getObject();

        List<IContributionItem> menu = new ArrayList<IContributionItem>();
        makeTableContributions(menu, table);

        return menu.toArray(new IContributionItem[menu.size()]);
    }

    // -------
    // Helpers
    // -------
    private void makeTableContributions(List<IContributionItem> menu, final DBSTable table)
    {
        menu.add(makeAction("Describe", new TableAnalysisRunner(table) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
            {
                sql.append(String.format(SQL_DESCRIBE, DBUtils.getObjectFullName(table)));
            }
        }));

        if (table instanceof DB2Table) {
            menu.add(makeAction("Reorg", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                {
                    sql.append(String.format(SQL_REORG, DBUtils.getObjectFullName(table)));
                }
            }));

            menu.add(makeAction("Runstats", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                {
                    sql.append(String.format(SQL_RUNSTATS, DBUtils.getObjectFullName(table)));
                }
            }));

            menu.add(makeAction("Set Integrity", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                {
                    sql.append(String.format(SQL_INTEGRITY, DBUtils.getObjectFullName(table)));
                }
            }));
        }

    }

    private abstract static class TableAnalysisRunner extends DBRRunnableWithResult<String> {
        final DBSEntity table;

        protected TableAnalysisRunner(DBSEntity table)
        {
            this.table = table;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            StringBuilder sql = new StringBuilder(100);
            try {
                generateSQL(monitor, sql);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
            result = sql.toString();
        }

        protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException;

    }

    private static ContributionItem makeAction(String text, final DBRRunnableWithResult<String> runnable)
    {
        return new ActionContributionItem(new Action(text, DBIcon.SQL_TEXT.getImageDescriptor()) {
            @Override
            public void run()
            {
                DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), runnable);
                String sql = runnable.getResult();
                IEditorPart activeEditor = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                if (activeEditor instanceof AbstractTextEditor) {
                    AbstractTextEditor textEditor = (AbstractTextEditor) activeEditor;
                    ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
                    IDocumentProvider provider = textEditor.getDocumentProvider();
                    IDocument doc = provider.getDocument(activeEditor.getEditorInput());
                    try {
                        doc.replace(selection.getOffset(), selection.getLength(), sql);
                    } catch (BadLocationException e) {
                        LOG.warn(e);
                    }
                    activeEditor.setFocus();
                }
                UIUtils.setClipboardContents(DBeaverUI.getActiveWorkbenchShell().getDisplay(), TextTransfer.getInstance(), sql);
            }
        });
    }

    static IStructuredSelection getSelectionFromPart(IWorkbenchPart part)
    {
        if (part == null) {
            return null;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return null;
        }
        ISelection selection = selectionProvider.getSelection();
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return null;
        }
        return (IStructuredSelection) selection;
    }

}
