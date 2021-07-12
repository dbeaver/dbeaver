/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql.variables;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContextListener;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.registry.SQLQueryParameterRegistry;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * SQLVariablesPanel
 */
public class SQLVariablesPanel extends Composite implements DBCScriptContextListener {

    static protected final Log log = Log.getLog(SQLVariablesPanel.class);

    private final SQLEditor mainEditor;
    private SQLEditorBase valueEditor;
    private TableViewer varsTable;
    private boolean showParameters;
    private boolean saveInProgress;
    private DBCScriptContext.VariableInfo curVariable;

    public SQLVariablesPanel(Composite parent, SQLEditor editor)
    {
        super(parent, SWT.NONE);
        this.mainEditor = editor;

        setLayout(new FillLayout());
    }

    private void createControls() {
        mainEditor.getGlobalScriptContext().addListener(this);
        addDisposeListener(e -> mainEditor.getGlobalScriptContext().removeListener(this));

        SashForm sash = new SashForm(this, SWT.VERTICAL);

        // Variables table
        {
            VariableListControl variableListControl = new VariableListControl(sash);
            variableListControl.createOrSubstituteProgressPanel(mainEditor.getSite());
        }

        // Editor
        {
            Composite editorGroup = UIUtils.createPlaceholder(sash, 1);

            UIUtils.createControlLabel(editorGroup, "Value");

            Composite editorPH = new Composite(editorGroup, SWT.NONE);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 3;
            gd.horizontalSpan = 1;
            gd.minimumHeight = 100;
            gd.minimumWidth = 100;
            editorPH.setLayoutData(gd);
            editorPH.setLayout(new FillLayout());

            valueEditor = new SQLEditorBase() {
                @Nullable
                @Override
                public DBCExecutionContext getExecutionContext() {
                    return mainEditor.getExecutionContext();
                }

                @Override
                public void createPartControl(Composite parent) {
                    super.createPartControl(parent);
                    getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES).setEnabled(false);
                }

                @Override
                public boolean isFoldingEnabled() {
                    return false;
                }
            };
            try {
                valueEditor.init(new SubEditorSite(mainEditor.getSite()),
                    new StringEditorInput("Variable value", "", true, GeneralUtils.getDefaultFileEncoding()));
            } catch (PartInitException e) {
                log.error(e);
            }
            valueEditor.createPartControl(editorPH);
            valueEditor.setWordWrap(true);
            valueEditor.reloadSyntaxRules();

            //valueEditor.getEditorControl().setEnabled(false);

            valueEditor.getEditorControlWrapper().setLayoutData(new GridData(GridData.FILL_BOTH));
            StyledText editorControl = valueEditor.getEditorControl();
            TextEditorUtils.enableHostEditorKeyBindingsSupport(mainEditor.getSite(), editorControl);
            if (editorControl != null) {
                editorControl.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        saveVariableValue(editorControl);
                    }
                });
                //editorControl.addDisposeListener(e -> saveVariableValue(editorControl));
            }
        }

        sash.setWeights(new int[] { 600, 400 });
    }

    private void saveVariableValue(StyledText editorControl) {
        String varValue = editorControl.getText();

        if (curVariable != null) {
            saveInProgress = true;
            try {
                curVariable.value = varValue;
                mainEditor.getGlobalScriptContext().setVariable(
                    curVariable.name,
                    varValue);
                varsTable.refresh();
            } finally {
                saveInProgress = false;
            }
        }
    }

    private void editCurrentVariable() {
        ISelection selection = varsTable.getSelection();
        StyledText editorControl = valueEditor.getEditorControl();
        if (editorControl == null) {
            return;
        }
        if (!selection.isEmpty()) {
            //TableItem item = varsTable.getItem(selectionIndex);
            curVariable = (DBCScriptContext.VariableInfo) ((IStructuredSelection)selection).getFirstElement();

            StringEditorInput sqlInput = new StringEditorInput(
                "Variable " + curVariable.name,
                CommonUtils.toString(curVariable.value),
                false,
                GeneralUtils.DEFAULT_ENCODING
                );
            valueEditor.setInput(sqlInput);
            valueEditor.reloadSyntaxRules();
        }
    }

    public void refreshVariables() {
        if (varsTable == null) {
            createControls();
        }

        SQLScriptContext context = mainEditor.getGlobalScriptContext();

        //varsTable.removeAll();
        List<DBCScriptContext.VariableInfo> variables = context.getVariables();
        if (showParameters) {
            for (SQLQueryParameterRegistry.ParameterInfo param : SQLQueryParameterRegistry.getInstance().getAllParameters()) {
                if (context.hasVariable(param.name)) {
                    continue;
                }
                Object parameterValue = context.getParameterDefaultValue(param.name);
                if (parameterValue == null) {
                    parameterValue = param.value;
                }
                variables.add(new DBCScriptContext.VariableInfo(
                    param.name,
                    parameterValue,
                    DBCScriptContext.VariableType.PARAMETER));
            }
        }

        varsTable.setInput(variables);
        //UIUtils.packColumns(varsTable.getTable(), true);

        valueEditor.setInput(new StringEditorInput(
            "Variable",
            "",
            true,
            GeneralUtils.DEFAULT_ENCODING
        ));
        valueEditor.reloadSyntaxRules();
    }

    @Override
    public void variableChanged(ContextAction action, DBCScriptContext.VariableInfo variable) {
        if (saveInProgress) {
            return;
        }
        UIUtils.asyncExec(this::refreshVariables);
    }

    @Override
    public void parameterChanged(ContextAction action, String name, Object value) {
        if (saveInProgress) {
            return;
        }
        UIUtils.asyncExec(this::refreshVariables);
    }

    private class VariableListControl extends ProgressPageControl {

        private final ISearchExecutor searcher;
        private Action addAction;
        private Action deleteAction;

        public VariableListControl(Composite parent) {
            super(parent, SWT.SHEET);
            searcher = new ISearchExecutor() {
                @Override
                public boolean performSearch(String searchString, int options) {
                    try {
                        PatternFilter searchFilter = new PatternFilter() {
                            protected boolean isLeafMatch(Viewer viewer, Object element) {
                                DBCScriptContext.VariableInfo variable = (DBCScriptContext.VariableInfo) element;

                                return wordMatches(variable.name) || wordMatches(CommonUtils.toString(variable.value));
                            }
                        };
                        searchFilter.setPattern(searchString);
                            //(options & SEARCH_CASE_SENSITIVE) != 0);
                        varsTable.setFilters(new ViewerFilter[]{ searchFilter });
                        return true;
                    } catch (PatternSyntaxException e) {
                        log.error(e.getMessage());
                        return false;
                    }
                }

                @Override
                public void cancelSearch() {
                    varsTable.setFilters(new ViewerFilter[0]);
                }
            };

            varsTable = new TableViewer(this, SWT.SINGLE | SWT.FULL_SELECTION);
            varsTable.getTable().setHeaderVisible(true);
            varsTable.getTable().setLinesVisible(true);
            varsTable.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

            ViewerColumnController columnController = new ViewerColumnController("sqlVariablesViewer", varsTable);

            varsTable.setContentProvider(new ListContentProvider());

            columnController.addColumn("Variable", "Variable or parameter name", SWT.LEFT, true, true, new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBCScriptContext.VariableInfo) element).name;
                }
            });
            columnController.addColumn("Value", "Variable or parameter value", SWT.LEFT, true, true, new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return CommonUtils.toString(((DBCScriptContext.VariableInfo) element).value);
                }
            });
            columnController.addColumn("Type", "Variable type", SWT.LEFT, true, true, new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBCScriptContext.VariableInfo) element).type.getTitle();
                }
            });

            columnController.createColumns(true);

            varsTable.addSelectionChangedListener(event -> {
                if (deleteAction != null) {
                    Object varElement = event.getSelection().isEmpty() ?
                        null : ((IStructuredSelection) varsTable.getSelection()).getFirstElement();

                    deleteAction.setEnabled(
                        varElement instanceof DBCScriptContext.VariableInfo &&
                        ((DBCScriptContext.VariableInfo) varElement).type != DBCScriptContext.VariableType.PARAMETER);
                    updateActions();
                }
                editCurrentVariable();
            });
            NavigatorUtils.createContextMenu(mainEditor.getSite(), varsTable, manager -> {} );
        }

        @Override
        protected ISearchExecutor getSearchRunner() {
            return searcher;
        }

        protected void addSearchAction(IContributionManager contributionManager) {
            contributionManager.add(new Action("Find variable", DBeaverIcons.getImageDescriptor(UIIcon.SEARCH)) {
                @Override
                public void run() {
                    performSearch(SearchType.NONE);
                }
            });
        }

        @Override
        protected void createSearchControls() {
            super.createSearchControls();
            Text textControl = getSearchTextControl();
            if (textControl != null) {
                TextEditorUtils.enableHostEditorKeyBindingsSupport(mainEditor.getSite(), textControl);
            }
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);

            addAction = new Action("Add variable", DBeaverIcons.getImageDescriptor(UIIcon.ADD)) {
                @Override
                public void run() {
                    AssignVariableAction action = new AssignVariableAction(mainEditor, "");
                    action.setEditable(true);
                    action.run();
                }
            };
            contributionManager.add(addAction);
            deleteAction = new Action("Delete variable", DBeaverIcons.getImageDescriptor(UIIcon.DELETE)) {
                @Override
                public void run() {
                    if (!varsTable.getSelection().isEmpty()) {
                        Object varElement = ((IStructuredSelection) varsTable.getSelection()).getFirstElement();
                        if (varElement instanceof DBCScriptContext.VariableInfo) {
                            RemoveVariableAction action = new RemoveVariableAction(mainEditor, ((DBCScriptContext.VariableInfo) varElement).name);
                            action.run();
                        }
                    }
                }
            };
            deleteAction.setEnabled(false);
            contributionManager.add(deleteAction);

            Action showParamsAction = new Action("Show parameters", Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    showParameters = !showParameters;
                    refreshVariables();
                }
            };
            showParamsAction.setChecked(showParameters);
            showParamsAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SQL_PARAMETER));
            showParamsAction.setDescription("Show query parameters");
            contributionManager.add(ActionUtils.makeActionContribution(showParamsAction, true));
        }
    }
}