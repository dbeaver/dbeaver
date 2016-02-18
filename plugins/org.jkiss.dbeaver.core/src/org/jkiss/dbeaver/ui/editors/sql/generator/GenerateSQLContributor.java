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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.eclipse.jface.action.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetSelection;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class GenerateSQLContributor extends CompoundContributionItem {

    static protected final Log log = Log.getLog(GenerateSQLContributor.class);

    @Override
    protected IContributionItem[] getContributionItems()
    {
        IWorkbenchPart part = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart();
        IStructuredSelection structuredSelection = GenerateSQLContributor.getSelectionFromPart(part);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return new IContributionItem[0];
        }

        List<IContributionItem> menu = new ArrayList<>();
        if (structuredSelection instanceof IResultSetSelection) {
            // Results
            makeResultSetContributions(menu, (IResultSetSelection) structuredSelection);

        } else {
            final DBSObject object =
                ((DBNDatabaseNode)RuntimeUtils.getObjectAdapter(structuredSelection.getFirstElement(), DBNNode.class)).getObject();
            makeTableContributions(menu, object);
        }
        return menu.toArray(new IContributionItem[menu.size()]);
    }

    private void makeTableContributions(List<IContributionItem> menu, final DBSObject object)
    {
        if (object instanceof DBSTable) {
            DBSTable table = (DBSTable)object;
            // Table
            menu.add(makeAction("SELECT ", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    sql.append("SELECT ");
                    boolean hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor)) {
                        if (attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                            continue;
                        }
                        if (hasAttr) sql.append(", ");
                        sql.append(DBUtils.getObjectFullName(attr));
                        hasAttr = true;
                    }
                    sql.append("\nFROM ").append(DBUtils.getObjectFullName(object));
                    sql.append(";\n");
                }
            }));
            menu.add(makeAction("INSERT ", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    sql.append("INSERT INTO ").append(DBUtils.getObjectFullName(object)).append("\n(");
                    boolean hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor)) {
                        if (attr.isPseudoAttribute() || attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                            continue;
                        }
                        if (hasAttr) sql.append(", ");
                        sql.append(DBUtils.getObjectFullName(attr));
                        hasAttr = true;
                    }
                    sql.append(")\nVALUES(");
                    hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor)) {
                        if (attr.isPseudoAttribute() || attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                            continue;
                        }
                        if (hasAttr) sql.append(", ");
                        appendDefaultValue(sql, attr);
                        hasAttr = true;
                    }
                    sql.append(");\n");
                }

            }));
            menu.add(makeAction("UPDATE ", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor);
                    sql.append("UPDATE ").append(DBUtils.getObjectFullName(object))
                        .append("\nSET ");
                    boolean hasAttr = false;
                    for (DBSEntityAttribute attr : getValueAttributes(monitor, keyAttributes)) {
                        if (attr.isPseudoAttribute() || attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                            continue;
                        }
                        if (hasAttr) sql.append(", ");
                        sql.append(DBUtils.getObjectFullName(attr)).append("=");
                        appendDefaultValue(sql, attr);
                        hasAttr = true;
                    }
                    if (!CommonUtils.isEmpty(keyAttributes)) {
                        sql.append("\nWHERE ");
                        hasAttr = false;
                        for (DBSEntityAttribute attr : keyAttributes) {
                            if (hasAttr) sql.append(" AND ");
                            sql.append(DBUtils.getObjectFullName(attr)).append("=");
                            appendDefaultValue(sql, attr);
                            hasAttr = true;
                        }
                    }
                    sql.append(";\n");
                }
            }));
            menu.add(makeAction("DELETE ", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    sql.append("DELETE FROM  ").append(DBUtils.getObjectFullName(object))
                        .append("\nWHERE ");
                    Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor);
                    if (CommonUtils.isEmpty(keyAttributes)) {
                        keyAttributes = getAllAttributes(monitor);
                    }
                    boolean hasAttr = false;
                    for (DBSEntityAttribute attr : keyAttributes) {
                        if (hasAttr) sql.append(" AND ");
                        sql.append(DBUtils.getObjectFullName(attr)).append("=");
                        appendDefaultValue(sql, attr);
                        hasAttr = true;
                    }
                    sql.append(";\n");
                }
            }));
            menu.add(makeAction("MERGE", new TableAnalysisRunner(table) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    boolean hasAttr = false;

                    sql.append("MERGE INTO ").append(DBUtils.getObjectFullName(object)).append(" AS tgt\n");
                    sql.append("USING SOURCE_TABLE AS src\n");
                    Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor);
                    if (!CommonUtils.isEmpty(keyAttributes)) {
                        sql.append("ON (");
                        for (DBSEntityAttribute attr : keyAttributes) {
                            if (hasAttr) sql.append(" AND ");
                            sql.append("tgt.").append(DBUtils.getQuotedIdentifier(attr))
                                .append("=src.").append(DBUtils.getQuotedIdentifier(attr));
                            hasAttr = true;
                        }
                        sql.append(")\n");
                    }
                    sql.append("WHEN MATCHED\nTHEN UPDATE SET\n");
                    hasAttr = false;
                    for (DBSEntityAttribute attr : getValueAttributes(monitor, keyAttributes)) {
                        if (hasAttr) sql.append(", ");
                        sql.append("tgt.").append(DBUtils.getQuotedIdentifier(attr))
                            .append("=src.").append(DBUtils.getQuotedIdentifier(attr));
                        hasAttr = true;
                    }
                    sql.append("\nWHEN NOT MATCHED\nTHEN INSERT (");
                    hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor)) {
                        if (hasAttr) sql.append(", ");
                        sql.append(DBUtils.getQuotedIdentifier(attr));
                        hasAttr = true;
                    }
                    sql.append(")\nVALUES (");
                    hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor)) {
                        if (hasAttr) sql.append(", ");
                        sql.append("src.").append(DBUtils.getQuotedIdentifier(attr));
                        hasAttr = true;
                    }
                    sql.append(");\n");
                }
            }));
        }
        if (object instanceof DBPScriptObject) {
            final DBPScriptObject so = (DBPScriptObject)object;
            if (menu.size() > 0) {
                menu.add(new Separator());
            }
            menu.add(makeAction("DDL", new SQLGenerator<DBPScriptObject>(so) {
                @Override
                public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException {
                    String definitionText = so.getObjectDefinitionText(monitor);
                    sql.append(definitionText);
                }
            }));
        }
    }

    private void makeResultSetContributions(List<IContributionItem> menu, IResultSetSelection rss)
    {
        final IResultSetController rsv = rss.getController();
        DBSDataContainer dataContainer = rsv.getDataContainer();
        DBSEntity entity = dataContainer instanceof DBSEntity ? (DBSEntity) dataContainer : null;
        if (entity == null) {
            entity = rsv.getModel().getSingleSource();
        }
        if (entity != null) {
            final Collection<ResultSetRow> selectedRows = rss.getSelectedRows();
            if (!CommonUtils.isEmpty(selectedRows)) {
                menu.add(makeAction("SELECT by Unique Key", new TableAnalysisRunner(entity) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                    {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor);
                            sql.append("SELECT ");
                            boolean hasAttr = false;
                            for (DBSEntityAttribute attr : getValueAttributes(monitor, keyAttributes)) {
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr));
                                hasAttr = true;
                            }
                            sql.append("\nFROM ").append(DBUtils.getObjectFullName(object));
                            sql.append("\nWHERE ");
                            hasAttr = false;
                            for (DBSEntityAttribute attr : keyAttributes) {
                                if (hasAttr) sql.append(" AND ");
                                DBDAttributeBinding binding = rsv.getModel().getAttributeBinding(attr);
                                sql.append(DBUtils.getObjectFullName(attr)).append("=");
                                if (binding == null) {
                                    appendDefaultValue(sql, attr);
                                } else {
                                    appendAttributeValue(rsv, sql, binding, firstRow);
                                }
                                hasAttr = true;
                            }
                            sql.append(";\n");
                        }
                    }
                }));
                menu.add(makeAction("INSERT", new TableAnalysisRunner(entity) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                    {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<? extends DBSEntityAttribute> allAttributes = getAllAttributes(monitor);
                            sql.append("INSERT INTO ").append(DBUtils.getObjectFullName(object));
                            sql.append("\n(");
                            boolean hasAttr = false;
                            for (DBSEntityAttribute attr : allAttributes) {
                                if (attr.isPseudoAttribute() || attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                                    continue;
                                }
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr));
                                hasAttr = true;
                            }
                            sql.append(")\nVALUES(");
                            hasAttr = false;
                            for (DBSEntityAttribute attr : allAttributes) {
                                if (attr.isPseudoAttribute() || attr instanceof DBPHiddenObject && ((DBPHiddenObject) attr).isHidden()) {
                                    continue;
                                }
                                if (hasAttr) sql.append(", ");
                                DBDAttributeBinding binding = rsv.getModel().getAttributeBinding(attr);
                                if (binding == null) {
                                    appendDefaultValue(sql, attr);
                                } else {
                                    appendAttributeValue(rsv, sql, binding, firstRow);
                                }
                                hasAttr = true;
                            }
                            sql.append(");\n");
                        }
                    }
                }));

                menu.add(makeAction("DELETE by Unique Key", new TableAnalysisRunner(entity) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql) throws DBException
                    {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor);
                            sql.append("DELETE FROM ").append(DBUtils.getObjectFullName(object));
                            sql.append("\nWHERE ");
                            boolean hasAttr = false;
                            for (DBSEntityAttribute attr : keyAttributes) {
                                if (hasAttr) sql.append(" AND ");
                                DBDAttributeBinding binding = rsv.getModel().getAttributeBinding(attr);
                                sql.append(DBUtils.getObjectFullName(attr)).append("=");
                                if (binding == null) {
                                    appendDefaultValue(sql, attr);
                                } else {
                                    appendAttributeValue(rsv, sql, binding, firstRow);
                                }
                                hasAttr = true;
                            }
                            sql.append(";\n");
                        }
                    }
                }));
            }
        }
    }

    public static boolean hasContributions(IStructuredSelection selection) {
        // Table
        DBNNode node = RuntimeUtils.getObjectAdapter(selection.getFirstElement(), DBNNode.class);
        if (node instanceof DBNDatabaseNode) {
            DBSObject object = ((DBNDatabaseNode) node).getObject();
            if (object instanceof DBSTable || object instanceof DBPScriptObject) {
                return true;
            }
        }
        return false;
    }

    private abstract static class SQLGenerator<T extends DBPObject> extends DBRRunnableWithResult<String> {
        final protected T object;

        protected SQLGenerator(T object)
        {
            this.object = object;
        }

        public T getObject() {
            return object;
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

        protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql)
            throws DBException;

    }

    private abstract static class TableAnalysisRunner extends SQLGenerator<DBSEntity> {

        protected TableAnalysisRunner(DBSEntity entity)
        {
            super(entity);
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

        protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql)
            throws DBException;

        protected Collection<? extends DBSEntityAttribute> getAllAttributes(DBRProgressMonitor monitor) throws DBException
        {
            return CommonUtils.safeCollection(object.getAttributes(monitor));
        }

        protected Collection<? extends DBSEntityAttribute> getKeyAttributes(DBRProgressMonitor monitor) throws DBException
        {
            return DBUtils.getBestTableIdentifier(monitor, object);
        }

        protected Collection<? extends DBSEntityAttribute> getValueAttributes(DBRProgressMonitor monitor, Collection<? extends DBSEntityAttribute> keyAttributes) throws DBException
        {
            if (CommonUtils.isEmpty(keyAttributes)) {
                return getAllAttributes(monitor);
            }
            List<DBSEntityAttribute> valueAttributes = new ArrayList<>(getAllAttributes(monitor));
            for (Iterator<DBSEntityAttribute> iter = valueAttributes.iterator(); iter.hasNext(); ) {
                if (keyAttributes.contains(iter.next())) {
                    iter.remove();
                }
            }
            return valueAttributes;
        }

        protected void appendDefaultValue(StringBuilder sql, DBSEntityAttribute attr)
        {
            if (!CommonUtils.isEmpty(attr.getDefaultValue())) {
                sql.append(attr.getDefaultValue());
            } else {
                switch (attr.getDataKind()) {
                    case BOOLEAN:
                        sql.append("false");
                        break;
                    case NUMERIC:
                        sql.append("0");
                        break;
                    case STRING:
                    case DATETIME:
                        sql.append("''");
                        break;
                    default:
                        sql.append("?");
                        break;
                }
            }
        }

        protected void appendAttributeValue(IResultSetController rsv, StringBuilder sql, DBDAttributeBinding binding, ResultSetRow row)
        {
            DBPDataSource dataSource = rsv.getDataContainer().getDataSource();
            Object value = rsv.getModel().getCellValue(binding, row);
            sql.append(
                SQLUtils.convertValueToSQL(dataSource, binding.getAttribute(), value));
        }
    }

    private static <T extends DBPObject> ContributionItem makeAction(String text, final SQLGenerator<T> runnable)
    {
        return new ActionContributionItem(
            new Action(text, DBeaverIcons.getImageDescriptor(UIIcon.SQL_TEXT)) {
                @Override
                public void run()
                {
                    DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), runnable);
                    String sql = runnable.getResult();
                    if (sql == null) {
                        return;
                    }
                    IEditorPart activeEditor = DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                    boolean showDialog = true;
                    if (activeEditor instanceof AbstractTextEditor) {
                        AbstractTextEditor textEditor = (AbstractTextEditor)activeEditor;
                        ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();
                        IDocumentProvider provider=textEditor.getDocumentProvider();
                        IDocument doc = provider.getDocument(activeEditor.getEditorInput());
                        try {
                            sql = GeneralUtils.getDefaultLineSeparator() + sql;
                            doc.replace(selection.getOffset(), selection.getLength(), sql);
                            textEditor.getSelectionProvider().setSelection(
                                new TextSelection(doc, selection.getOffset() + sql.length(), 0));
                        } catch (BadLocationException e) {
                            log.warn(e);
                        }
                        activeEditor.setFocus();
                        showDialog = false;
                    }
                    if (showDialog) {
                        DBPDataSource dataSource = null;
                        if (runnable.getObject() instanceof DBSObject) {
                            dataSource = ((DBSObject)runnable.getObject()).getDataSource();
                        }
                        if (dataSource != null) {
                            ViewSQLDialog dialog = new ViewSQLDialog(
                                DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                                dataSource.getDefaultContext(false),
                                "Generated SQL",
                                null,
                                sql);
                            dialog.open();
                        }
                    } else {
                        UIUtils.setClipboardContents(DBeaverUI.getActiveWorkbenchShell().getDisplay(), TextTransfer.getInstance(), sql);
                    }
                }
        });
    }

    @Nullable
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
        return (IStructuredSelection)selection;
    }

}
