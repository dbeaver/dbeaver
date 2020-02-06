/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorProcedureCall;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorSelect;
import org.jkiss.dbeaver.model.sql.registry.SQLGeneratorConfigurationRegistry;
import org.jkiss.dbeaver.model.sql.registry.SQLGeneratorDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetSelection;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class GenerateSQLContributor extends CompoundContributionItem {

    static protected final Log log = Log.getLog(GenerateSQLContributor.class);

    //////////////////////////////////////////////////////////
    // Contributors

    @Override
    protected IContributionItem[] getContributionItems() {
        IWorkbenchPart part = UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart();
        IStructuredSelection structuredSelection = NavigatorUtils.getSelectionFromPart(part);
        if (structuredSelection == null || structuredSelection.isEmpty()) {
            return new IContributionItem[0];
        }

        List<IContributionItem> menu = new ArrayList<>();
        if (structuredSelection instanceof IResultSetSelection) {
            // Results
            makeResultSetContributions(menu, (IResultSetSelection) structuredSelection);

        } else {
            List<DBPObject> objects = new ArrayList<>();
            for (Object obj : structuredSelection.toList()) {
                if (obj instanceof DBSWrapper) {
                    objects.add(((DBSWrapper) obj).getObject());
                } else if (obj instanceof DBPObject) {
                    objects.add((DBPObject) obj);
                }
            }
            List<SQLGeneratorDescriptor> generators = SQLGeneratorConfigurationRegistry.getInstance().getApplicableGenerators(objects, structuredSelection);
            int lastGrand = 0;
            for (SQLGeneratorDescriptor gen : generators) {
                int order = gen.getOrder();
                if (order > 0 && order / 1000 > lastGrand) {
                    menu.add(new Separator());
                }
                lastGrand = order / 1000;

                menu.add(makeAction(gen.getLabel(), gen, objects));
            }
        }
        return menu.toArray(new IContributionItem[0]);
    }

    private void makeResultSetContributions(List<IContributionItem> menu, IResultSetSelection rss) {
        final IResultSetController rsv = rss.getController();
        DBSDataContainer dataContainer = rsv.getDataContainer();
        final List<DBDAttributeBinding> visibleAttributes = rsv.getModel().getVisibleAttributes();
        final DBSEntity entity = rsv.getModel().getSingleSource();
        if (dataContainer != null && !visibleAttributes.isEmpty() && entity != null) {
            final List<ResultSetRow> selectedRows = new ArrayList<>(rss.getSelectedRows());
            if (!CommonUtils.isEmpty(selectedRows)) {

                List<IResultSetController> objects = new ArrayList<>();
                objects.add(rsv);
                List<SQLGeneratorDescriptor> generators = SQLGeneratorConfigurationRegistry.getInstance().getApplicableGenerators(objects, rsv);
                for (SQLGeneratorDescriptor gen : generators) {
                    if (gen.isMultiObject() && selectedRows.size() < 2) {
                        continue;
                    }
                    menu.add(makeAction(gen.getLabel(), gen, objects));
                }
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
        DBSObject object = RuntimeUtils.getObjectAdapter(selection.getFirstElement(), DBSObject.class);
        return object instanceof DBSTable || object instanceof DBPScriptObject;
    }

    private static ContributionItem makeAction(String text, SQLGeneratorDescriptor sqlGenerator, List<?> objects) {
        return new ActionContributionItem(
            new Action(text, DBeaverIcons.getImageDescriptor(UIIcon.SQL_TEXT)) {
                @Override
                public void run() {
                    IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
                    IEditorPart activeEditor = activePage.getActiveEditor();

                    DBCExecutionContext executionContext = null;
                    IWorkbenchPart activePart = activePage.getActivePart();
                    if (activePart != null) {
                        ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
                        if (selectionProvider != null) {
                            DBSObject selectedObject = NavigatorUtils.getSelectedObject(selectionProvider.getSelection());
                            if (selectedObject != null) {
                                executionContext = DBUtils.getDefaultContext(selectedObject, false);
                            }
                        }
                    }
                    if (executionContext == null && activeEditor instanceof DBPContextProvider) {
                        executionContext = ((DBPContextProvider) activeEditor).getExecutionContext();
                    }

                    if (executionContext != null) {
                        SQLGenerator<?> generator;
                        try {
                            generator = sqlGenerator.createGenerator(objects);
                        } catch (DBException e) {
                            DBWorkbench.getPlatformUI().showError("Generator create", "Can't create SQL generator '" + sqlGenerator.getId() + "'", e);
                            return;
                        }
                        ViewSQLDialog dialog = new GenerateSQLDialog(
                            activePage.getActivePart().getSite(),
                            executionContext,
                            generator);
                        dialog.open();
                    }
                }
            });
    }

    private static class GenerateSQLDialog extends ViewSQLDialog {

        private static final String PROP_USE_FQ_NAMES = "GenerateSQL.useFQNames";
        private static final String PROP_USE_COMPACT_SQL = "GenerateSQL.compactSQL";
        private final SQLGenerator<?> sqlGenerator;

        GenerateSQLDialog(IWorkbenchPartSite parentSite, DBCExecutionContext context, SQLGenerator<?> sqlGenerator) {
            super(parentSite, () -> context,
                "Generated SQL (" + context.getDataSource().getContainer().getName() + ")",
                null, "");
            this.sqlGenerator = sqlGenerator;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            sqlGenerator.setFullyQualifiedNames(
                getDialogBoundsSettings().get(PROP_USE_FQ_NAMES) == null ||
                    getDialogBoundsSettings().getBoolean(PROP_USE_FQ_NAMES));
            sqlGenerator.setCompactSQL(
                getDialogBoundsSettings().get(PROP_USE_COMPACT_SQL) != null &&
                    getDialogBoundsSettings().getBoolean(PROP_USE_COMPACT_SQL));
            UIUtils.runInUI(sqlGenerator);
            Object sql = sqlGenerator.getResult();
            if (sql != null) {
                setSQLText(CommonUtils.toString(sql));
            }

            Composite composite = super.createDialogArea(parent);

            Group settings = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
            Button useFQNames = UIUtils.createCheckbox(settings, "Use fully qualified names", sqlGenerator.isFullyQualifiedNames());
            useFQNames.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setFullyQualifiedNames(useFQNames.getSelection());
                    getDialogBoundsSettings().put(PROP_USE_FQ_NAMES, useFQNames.getSelection());

                    UIUtils.runInUI(sqlGenerator);
                    Object sql = sqlGenerator.getResult();
                    if (sql != null) {
                        setSQLText(CommonUtils.toString(sql));
                        updateSQL();
                    }
                }
            });
            Button useCompactSQL = UIUtils.createCheckbox(settings, "Compact SQL", sqlGenerator.isCompactSQL());
            useCompactSQL.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    sqlGenerator.setCompactSQL(useCompactSQL.getSelection());
                    getDialogBoundsSettings().put(PROP_USE_COMPACT_SQL, useCompactSQL.getSelection());

                    UIUtils.runInUI(sqlGenerator);
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

    ///////////////////////////////////////////////////
    // Generators

    @NotNull
    public static SQLGenerator<DBSEntity> SELECT_GENERATOR(final List<DBSEntity> entities, final boolean columnList) {
        SQLGeneratorSelect generatorSelect = new SQLGeneratorSelect();
        generatorSelect.initGenerator(entities);
        generatorSelect.setColumnList(columnList);
        return generatorSelect;
    }

    @NotNull
    public static SQLGenerator<DBSProcedure> CALL_GENERATOR(final List<DBSProcedure> entities) {
        SQLGeneratorProcedureCall procedureCall = new SQLGeneratorProcedureCall();
        procedureCall.initGenerator(entities);
        return procedureCall;
    }

}
