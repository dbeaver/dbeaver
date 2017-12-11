/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors.sql.generator;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetSelection;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GenerateSQLContributor extends CompoundContributionItem {

    static protected final Log log = Log.getLog(GenerateSQLContributor.class);

    //////////////////////////////////////////////////////////
    // Contributors

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
            List<DBSEntity> entities = new ArrayList<>();
            List<DBPScriptObject> scriptObjects = new ArrayList<>();
            for (Object sel : structuredSelection.toArray()) {
                final DBSObject object =
                    ((DBNDatabaseNode)RuntimeUtils.getObjectAdapter(sel, DBNNode.class)).getObject();
                if (object instanceof DBSEntity) {
                    entities.add((DBSEntity) object);
                }
                if (object instanceof DBPScriptObject) {
                    scriptObjects.add((DBPScriptObject) object);
                }
            }
            if (!entities.isEmpty()) {
                makeTableContributions(menu, entities);
            }
            if (!scriptObjects.isEmpty()) {
                makeScriptContributions(menu, scriptObjects);
            }
        }
        return menu.toArray(new IContributionItem[menu.size()]);
    }

    private void makeTableContributions(List<IContributionItem> menu, final List<DBSEntity> entities)
    {
        // Table
        menu.add(makeAction("SELECT ", SELECT_GENERATOR(entities, true)));
        menu.add(makeAction("INSERT ", INSERT_GENERATOR(entities)));
        menu.add(makeAction("UPDATE ", UPDATE_GENERATOR(entities)));
        menu.add(makeAction("DELETE ", DELETE_GENERATOR(entities)));
        menu.add(makeAction("MERGE", MERGE_GENERATOR(entities)));
    }

    private void makeScriptContributions(List<IContributionItem> menu, final List<DBPScriptObject> scriptObjects)
    {
        if (menu.size() > 0) {
            menu.add(new Separator());
        }
        menu.add(makeAction("DDL", new SQLGenerator<DBPScriptObject>(scriptObjects) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBPScriptObject object) throws DBException {
                if (sql.length() > 0) {
                    sql.append("\n");
                }
                Map<String, Object> options = new HashMap<>();
                options.put(DBPScriptObject.OPTION_REFRESH, true);
                addOptions(options);

                String definitionText = CommonUtils.notEmpty(object.getObjectDefinitionText(monitor, options)).trim();
                sql.append(definitionText);
                if (!definitionText.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                    sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
                }
                sql.append("\n");
                if (object instanceof DBPScriptObjectExt) {
                    String definition2 = CommonUtils.notEmpty(((DBPScriptObjectExt) object).getExtendedDefinitionText(monitor)).trim();
                    sql.append("\n");
                    sql.append(definition2);
                    if (!definition2.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                        sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
                    }
                    sql.append("\n");
                }
            }

            @Override
            protected void addOptions(Map<String, Object> options) {
                super.addOptions(options);
                options.put(DBPScriptObject.OPTION_INCLUDE_OBJECT_DROP, true);
            }
        }));
    }

    private void makeResultSetContributions(List<IContributionItem> menu, IResultSetSelection rss)
    {
        final IResultSetController rsv = rss.getController();
        DBSDataContainer dataContainer = rsv.getDataContainer();
        final List<DBDAttributeBinding> visibleAttributes = rsv.getModel().getVisibleAttributes();
        final DBSEntity entity = rsv.getModel().getSingleSource();
        if (dataContainer != null && !visibleAttributes.isEmpty() && entity != null) {
            final List<ResultSetRow> selectedRows = new ArrayList<>(rss.getSelectedRows());
            if (!CommonUtils.isEmpty(selectedRows)) {

                menu.add(makeAction("SELECT .. WHERE .. =", new ResultSetAnalysisRunner(dataContainer.getDataSource(), rsv.getModel()) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) throws DBException
                    {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
                            sql.append("SELECT ");
                            boolean hasAttr = false;
                            for (DBSAttributeBase attr : getAllAttributes(monitor, object)) {
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                                hasAttr = true;
                            }
                            sql.append("\nFROM ").append(getEntityName(entity));
                            sql.append("\nWHERE ");
                            hasAttr = false;
                            for (DBDAttributeBinding binding : keyAttributes) {
                                if (hasAttr) sql.append(" AND ");
                                appendValueCondition(rsv, sql, binding, firstRow);
                                hasAttr = true;
                            }
                            sql.append(";\n");
                        }
                    }

                }));
                if (selectedRows.size() > 1) {
                    menu.add(makeAction("SELECT .. WHERE .. IN", new ResultSetAnalysisRunner(dataContainer.getDataSource(), rsv.getModel()) {
                        @Override
                        public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) throws DBException
                        {
                            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
                            sql.append("SELECT ");
                            boolean hasAttr = false;
                            for (DBSAttributeBase attr : getAllAttributes(monitor, object)) {
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                                hasAttr = true;
                            }
                            sql.append("\nFROM ").append(getEntityName(entity));
                            sql.append("\nWHERE ");
                            boolean multiKey = keyAttributes.size() > 1;
                            if (multiKey) sql.append("(");
                            hasAttr = false;
                            for (DBDAttributeBinding binding : keyAttributes) {
                                if (hasAttr) sql.append(",");
                                sql.append(DBUtils.getObjectFullName(binding.getAttribute(), DBPEvaluationContext.DML));
                                hasAttr = true;
                            }
                            if (multiKey) sql.append(")");
                            sql.append(" IN (");
                            if (multiKey) sql.append("\n");
                            for (int i = 0; i < selectedRows.size(); i++) {
                                ResultSetRow firstRow = selectedRows.get(i);
                                if (multiKey) sql.append("(");
                                hasAttr = false;
                                for (DBDAttributeBinding binding : keyAttributes) {
                                    if (hasAttr) sql.append(",");
                                    appendAttributeValue(rsv, sql, binding, firstRow);
                                    hasAttr = true;
                                }
                                if (multiKey) sql.append(")");
                                if (i < selectedRows.size() - 1) sql.append(",");
                                if (multiKey) sql.append("\n");
                            }
                            sql.append(");\n");
                        }
                    }));
                }
                menu.add(makeAction("INSERT", new ResultSetAnalysisRunner(dataContainer.getDataSource(), rsv.getModel()) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) throws DBException {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<? extends DBSAttributeBase> allAttributes = getAllAttributes(monitor, object);
                            sql.append("INSERT INTO ").append(getEntityName(entity));
                            sql.append("\n(");
                            boolean hasAttr = false;
                            for (DBSAttributeBase attr : allAttributes) {
                                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                                    continue;
                                }
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                                hasAttr = true;
                            }
                            sql.append(")\nVALUES(");
                            hasAttr = false;
                            for (DBSAttributeBase attr : allAttributes) {
                                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
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

                menu.add(makeAction("UPDATE", new ResultSetAnalysisRunner(dataContainer.getDataSource(), rsv.getModel()) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) throws DBException {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
                            Collection<? extends DBSAttributeBase> valueAttributes = getValueAttributes(monitor, object, keyAttributes);
                            sql.append("UPDATE ").append(getEntityName(entity));
                            sql.append("\nSET ");
                            boolean hasAttr = false;
                            for (DBSAttributeBase attr : valueAttributes) {
                                if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                                    continue;
                                }
                                if (hasAttr) sql.append(", ");
                                sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                                DBDAttributeBinding binding = rsv.getModel().getAttributeBinding(attr);
                                if (binding == null) {
                                    appendDefaultValue(sql, attr);
                                } else {
                                    appendAttributeValue(rsv, sql, binding, firstRow);
                                }

                                hasAttr = true;
                            }
                            sql.append("\nWHERE ");
                            hasAttr = false;
                            for (DBDAttributeBinding attr : keyAttributes) {
                                if (hasAttr) sql.append(" AND ");
                                appendValueCondition(rsv, sql, attr, firstRow);
                                hasAttr = true;
                            }
                            sql.append(";\n");
                        }
                    }
                }));

                menu.add(makeAction("DELETE by Unique Key", new ResultSetAnalysisRunner(dataContainer.getDataSource(), rsv.getModel()) {
                    @Override
                    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object) throws DBException
                    {
                        for (ResultSetRow firstRow : selectedRows) {

                            Collection<DBDAttributeBinding> keyAttributes = getKeyAttributes(monitor, object);
                            sql.append("DELETE FROM ").append(getEntityName(entity));
                            sql.append("\nWHERE ");
                            boolean hasAttr = false;
                            for (DBDAttributeBinding binding : keyAttributes) {
                                if (hasAttr) sql.append(" AND ");
                                appendValueCondition(rsv, sql, binding, firstRow);
                                hasAttr = true;
                            }
                            sql.append(";\n");
                        }
                    }
                }));
            }
        } else {
            //if (dataContainer != null && !visibleAttributes.isEmpty() && entity != null)
            String message = dataContainer == null ? "no data container" :
                (visibleAttributes.isEmpty() ? "empty attribute list" : "can't resolve table");
            Action disabledAction = new Action("Not available - " + message) {
            };
            disabledAction.setEnabled(false);
            menu.add(new ActionContributionItem(disabledAction));
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

    public abstract static class SQLGenerator<OBJECT> extends DBRRunnableWithResult<String> {
        final protected List<OBJECT> objects;
        private boolean fullyQualifiedNames = true;

        SQLGenerator(List<OBJECT> objects)
        {
            this.objects = objects;
        }

        boolean isFullyQualifiedNames() {
            return fullyQualifiedNames;
        }

        void setFullyQualifiedNames(boolean fullyQualifiedNames) {
            this.fullyQualifiedNames = fullyQualifiedNames;
        }

        protected String getEntityName(DBSEntity entity) {
            if (fullyQualifiedNames) {
                return DBUtils.getObjectFullName(entity, DBPEvaluationContext.DML);
            } else {
                return DBUtils.getQuotedIdentifier(entity);
            }
        }

        protected void addOptions(Map<String, Object> options) {
            options.put(DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, isFullyQualifiedNames());
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            StringBuilder sql = new StringBuilder(100);
            try {
                for (OBJECT object : objects) {
                    generateSQL(monitor, sql, object);
                }
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
            result = sql.toString();
        }

        protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, OBJECT object)
            throws DBException;

    }

    private abstract static class BaseAnalysisRunner<OBJECT> extends SQLGenerator<OBJECT> {

        protected BaseAnalysisRunner(List<OBJECT> objects) {
            super(objects);
        }

        protected abstract Collection<? extends DBSAttributeBase> getAllAttributes(DBRProgressMonitor monitor, OBJECT object) throws DBException;

        protected abstract Collection<? extends DBSAttributeBase> getKeyAttributes(DBRProgressMonitor monitor, OBJECT object) throws DBException;

        protected Collection<? extends DBSAttributeBase> getValueAttributes(DBRProgressMonitor monitor, OBJECT object, Collection<? extends DBSAttributeBase> keyAttributes) throws DBException
        {
            if (CommonUtils.isEmpty(keyAttributes)) {
                return getAllAttributes(monitor, object);
            }
            List<DBSAttributeBase> valueAttributes = new ArrayList<>(getAllAttributes(monitor, object));
            for (Iterator<DBSAttributeBase> iter = valueAttributes.iterator(); iter.hasNext(); ) {
                if (keyAttributes.contains(iter.next())) {
                    iter.remove();
                }
            }
            return valueAttributes;
        }

        protected void appendDefaultValue(StringBuilder sql, DBSAttributeBase attr)
        {
            String defValue = null;
            if (attr instanceof DBSEntityAttribute) {
                defValue = ((DBSEntityAttribute) attr).getDefaultValue();
            }
            if (!CommonUtils.isEmpty(defValue)) {
                sql.append(defValue);
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
                    case CONTENT:
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
            DBPDataSource dataSource = binding.getDataSource();
            Object value = rsv.getModel().getCellValue(binding, row);
            sql.append(
                SQLUtils.convertValueToSQL(dataSource, binding.getAttribute(), value));
        }
    }

    private abstract static class TableAnalysisRunner extends BaseAnalysisRunner<DBSEntity> {

        protected TableAnalysisRunner(List<DBSEntity> entities)
        {
            super(entities);
        }

        protected Collection<? extends DBSEntityAttribute> getAllAttributes(DBRProgressMonitor monitor, DBSEntity object) throws DBException
        {
            return CommonUtils.safeCollection(object.getAttributes(monitor));
        }

        protected Collection<? extends DBSEntityAttribute> getKeyAttributes(DBRProgressMonitor monitor, DBSEntity object) throws DBException
        {
            return DBUtils.getBestTableIdentifier(monitor, object);
        }
    }

    private abstract static class ProcedureAnalysisRunner extends BaseAnalysisRunner<DBSProcedure> {

        protected ProcedureAnalysisRunner(List<DBSProcedure> entities)
        {
            super(entities);
        }

        protected Collection<? extends DBSEntityAttribute> getAllAttributes(DBRProgressMonitor monitor, DBSProcedure object) throws DBException
        {
            return Collections.emptyList();
        }

        protected Collection<? extends DBSEntityAttribute> getKeyAttributes(DBRProgressMonitor monitor, DBSProcedure object) throws DBException
        {
            return Collections.emptyList();
        }
    }

    private abstract static class ResultSetAnalysisRunner extends BaseAnalysisRunner<ResultSetModel> {

        ResultSetAnalysisRunner(DBPDataSource dataSource, ResultSetModel model)
        {
            super(Collections.singletonList(model));
        }

        protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, ResultSetModel object)
            throws DBException;

        protected Collection<? extends DBSAttributeBase> getAllAttributes(DBRProgressMonitor monitor, ResultSetModel object) throws DBException
        {
            return object.getVisibleAttributes();
        }

        void appendValueCondition(IResultSetController rsv, StringBuilder sql, DBDAttributeBinding binding, ResultSetRow firstRow) {
            Object value = rsv.getModel().getCellValue(binding, firstRow);
            sql.append(DBUtils.getObjectFullName(binding.getAttribute(), DBPEvaluationContext.DML));
            if (DBUtils.isNullValue(value)) {
                sql.append(" IS NULL");
            } else {
                sql.append("=");
                appendAttributeValue(rsv, sql, binding, firstRow);
            }
        }

        protected List<DBDAttributeBinding> getKeyAttributes(DBRProgressMonitor monitor, ResultSetModel object) throws DBException
        {
            final DBDRowIdentifier rowIdentifier = getDefaultRowIdentifier(object);
            if (rowIdentifier == null) {
                return Collections.emptyList();
            }
            return rowIdentifier.getAttributes();
        }

        @Nullable
        private DBDRowIdentifier getDefaultRowIdentifier(ResultSetModel object) {
            for (DBDAttributeBinding attr : object.getAttributes()) {
                DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
                if (rowIdentifier != null) {
                    return rowIdentifier;
                }
            }
            return null;
        }

    }

    private static ContributionItem makeAction(String text, final SQLGenerator<?> sqlGenerator)
    {
        return new ActionContributionItem(
            new Action(text, DBeaverIcons.getImageDescriptor(UIIcon.SQL_TEXT)) {
                @Override
                public void run()
                {
                    IWorkbenchPage activePage = DBeaverUI.getActiveWorkbenchWindow().getActivePage();
                    IEditorPart activeEditor = activePage.getActiveEditor();

                    DBPDataSource dataSource = null;
                    if (activeEditor instanceof DBPContextProvider) {
                        DBCExecutionContext context = ((DBPContextProvider) activeEditor).getExecutionContext();
                        if (context != null) {
                            dataSource = context.getDataSource();
                        }
                    }
                    if (dataSource == null) {
                        IWorkbenchPart activePart = activePage.getActivePart();
                        if (activePart != null) {
                            DBNNode selectedNode = NavigatorUtils.getSelectedNode(activePart.getSite().getSelectionProvider());
                            if (selectedNode instanceof DBNDatabaseNode) {
                                dataSource = ((DBNDatabaseNode) selectedNode).getDataSource();
                            }
                        }
                    }

                    if (dataSource != null) {
                        ViewSQLDialog dialog = new GenerateSQLDialog(
                            activePage.getActivePart().getSite(),
                            dataSource.getDefaultContext(false),
                            sqlGenerator);
                        dialog.open();
                    }
                }
        });
    }

    private static class GenerateSQLDialog extends ViewSQLDialog {

        private static final String PROP_USE_FQ_NAMES = "GenerateSQL.useFQNames";
        private final SQLGenerator<?> sqlGenerator;

        public GenerateSQLDialog(IWorkbenchPartSite parentSite, DBCExecutionContext context, SQLGenerator<?> sqlGenerator) {
            super(parentSite, context,
                "Generated SQL (" + context.getDataSource().getContainer().getName() + ")",
                null, "");
            this.sqlGenerator = sqlGenerator;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            sqlGenerator.setFullyQualifiedNames(
                getDialogBoundsSettings().get(PROP_USE_FQ_NAMES) == null ||
                    getDialogBoundsSettings().getBoolean(PROP_USE_FQ_NAMES));
            DBeaverUI.runInUI(sqlGenerator);
            Object sql = sqlGenerator.getResult();
            if (sql != null) {
                setSQLText(CommonUtils.toString(sql));
            }

            Composite composite = super.createDialogArea(parent);

            Group settings = UIUtils.createControlGroup(composite, "Settings", 1, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
            Button useFQNames = UIUtils.createCheckbox(settings, "Use fully qualified names", sqlGenerator.isFullyQualifiedNames());
            useFQNames.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setFullyQualifiedNames(useFQNames.getSelection());
                    getDialogBoundsSettings().put(PROP_USE_FQ_NAMES, useFQNames.getSelection());

                    DBeaverUI.runInUI(sqlGenerator);
                    Object sql = sqlGenerator.getResult();
                    if (sql != null) {
                        setSQLText(CommonUtils.toString(sql));
                        updateSQL();
                    }
                }
            });

            return composite;
        }
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

    ///////////////////////////////////////////////////
    // Generators


    @NotNull
    public static SQLGenerator<DBSEntity> SELECT_GENERATOR(final List<DBSEntity> entities, final boolean columnList) {
        return new TableAnalysisRunner(entities) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
                sql.append("SELECT ");
                if (!columnList) {
                    sql.append("* ");
                } else {
                    boolean hasAttr = false;
                    for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
                        if (DBUtils.isHiddenObject(attr)) {
                            continue;
                        }
                        if (hasAttr) sql.append(", ");
                        sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                        hasAttr = true;
                    }
                    sql.append("\n");
                }
                sql.append("FROM ").append(getEntityName(object));
                sql.append(";\n");
            }
        };
    }

    @NotNull
    private SQLGenerator<DBSEntity> DELETE_GENERATOR(final List<DBSEntity> entities) {
        return new TableAnalysisRunner(entities) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
                sql.append("DELETE FROM  ").append(getEntityName(object))
                    .append("\nWHERE ");
                Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
                if (CommonUtils.isEmpty(keyAttributes)) {
                    keyAttributes = getAllAttributes(monitor, object);
                }
                boolean hasAttr = false;
                for (DBSEntityAttribute attr : keyAttributes) {
                    if (hasAttr) sql.append(" AND ");
                    sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                    appendDefaultValue(sql, attr);
                    hasAttr = true;
                }
                sql.append(";\n");
            }
        };
    }

    @NotNull
    private static SQLGenerator<DBSEntity> INSERT_GENERATOR(final List<DBSEntity> entities) {
        return new TableAnalysisRunner(entities) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
                sql.append("INSERT INTO ").append(getEntityName(object)).append("\n(");
                boolean hasAttr = false;
                for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
                    if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    if (hasAttr) sql.append(", ");
                    sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML));
                    hasAttr = true;
                }
                sql.append(")\nVALUES(");
                hasAttr = false;
                for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
                    if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    if (hasAttr) sql.append(", ");
                    appendDefaultValue(sql, attr);
                    hasAttr = true;
                }
                sql.append(");\n");
            }

        };
    }

    @NotNull
    private static SQLGenerator<DBSEntity> UPDATE_GENERATOR(final List<DBSEntity> entities) {
        return new TableAnalysisRunner(entities) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
                Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
                sql.append("UPDATE ").append(getEntityName(object))
                    .append("\nSET ");
                boolean hasAttr = false;
                for (DBSAttributeBase attr : getValueAttributes(monitor, object, keyAttributes)) {
                    if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                        continue;
                    }
                    if (hasAttr) sql.append(", ");
                    sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                    appendDefaultValue(sql, attr);
                    hasAttr = true;
                }
                if (!CommonUtils.isEmpty(keyAttributes)) {
                    sql.append("\nWHERE ");
                    hasAttr = false;
                    for (DBSEntityAttribute attr : keyAttributes) {
                        if (hasAttr) sql.append(" AND ");
                        sql.append(DBUtils.getObjectFullName(attr, DBPEvaluationContext.DML)).append("=");
                        appendDefaultValue(sql, attr);
                        hasAttr = true;
                    }
                }
                sql.append(";\n");
            }
        };
    }

    @NotNull
    private static SQLGenerator<DBSEntity> MERGE_GENERATOR(final List<DBSEntity> entities) {
        return new TableAnalysisRunner(entities) {
            @Override
            public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntity object) throws DBException {
                boolean hasAttr = false;

                sql.append("MERGE INTO ").append(getEntityName(object)).append(" AS tgt\n");
                sql.append("USING SOURCE_TABLE AS src\n");
                Collection<? extends DBSEntityAttribute> keyAttributes = getKeyAttributes(monitor, object);
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
                for (DBSAttributeBase attr : getValueAttributes(monitor, object, keyAttributes)) {
                    if (hasAttr) sql.append(", ");
                    sql.append("tgt.").append(DBUtils.getQuotedIdentifier(object.getDataSource(), attr.getName()))
                        .append("=src.").append(DBUtils.getQuotedIdentifier(object.getDataSource(), attr.getName()));
                    hasAttr = true;
                }
                sql.append("\nWHEN NOT MATCHED\nTHEN INSERT (");
                hasAttr = false;
                for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
                    if (hasAttr) sql.append(", ");
                    sql.append(DBUtils.getQuotedIdentifier(attr));
                    hasAttr = true;
                }
                sql.append(")\nVALUES (");
                hasAttr = false;
                for (DBSEntityAttribute attr : getAllAttributes(monitor, object)) {
                    if (hasAttr) sql.append(", ");
                    sql.append("src.").append(DBUtils.getQuotedIdentifier(attr));
                    hasAttr = true;
                }
                sql.append(");\n");
            }
        };
    }

    @NotNull
    public static SQLGenerator<DBSProcedure> CALL_GENERATOR(final List<DBSProcedure> entities) {
        return new ProcedureAnalysisRunner(entities) {

            @Override
            protected void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSProcedure proc) throws DBException {
                Collection<? extends DBSProcedureParameter> parameters = proc.getParameters(monitor);
                DBPDataSource dataSource = proc.getDataSource();
                if (dataSource instanceof SQLDataSource) {
                    SQLDataSource sqlDataSource = (SQLDataSource) dataSource;
                    SQLDialect sqlDialect = sqlDataSource.getSQLDialect();
                    sqlDialect.generateStoredProcedureCall(sql, proc, parameters);
                }
            }
        };
    }
}
