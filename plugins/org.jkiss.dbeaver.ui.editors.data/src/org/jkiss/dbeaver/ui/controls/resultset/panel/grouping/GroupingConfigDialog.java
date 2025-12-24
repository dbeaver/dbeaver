/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.StringEditorTableFactory;
import org.jkiss.dbeaver.ui.controls.StringEditorTableUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Grouping configuration dialog
 */
public class GroupingConfigDialog extends BaseDialog {
    private static final String DIALOG_ID = "DBeaver.GroupingConfigDialog"; //$NON-NLS-1$

    private final GroupingResultsContainer resultsContainer;
    private Table columnsTable;
    private Table functionsTable;

    public GroupingConfigDialog(Shell parentShell, GroupingResultsContainer resultsContainer) {
        super(parentShell, "Grouping configuration", null);
        this.resultsContainer = resultsContainer;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        List<String> allColumnNames = new ArrayList<>();
        for (DBDAttributeBinding attr : resultsContainer.getOwnerPresentation().getController().getModel().getAttributes()) {
            allColumnNames.add(getAttributeBindingName(attr));
        }
        List<String> proposals = new ArrayList<>(allColumnNames);
        StringContentProposalProvider proposalProvider = new StringContentProposalProvider(new String[0]);
        proposalProvider.setProposals(proposals.toArray(new String[0]));

        columnsTable = createColumnsTable(parent, proposalProvider, resultsContainer.getGroupAttributes(), allColumnNames);

        List<String> defaultFunctions = List.of("COUNT", "SUM", "AVG", "MAX", "MIN");
        proposals.addAll(defaultFunctions);
        proposalProvider.setProposals(proposals.toArray(new String[0]));
        functionsTable = createFunctionsTable(
            parent,
            proposalProvider, resultsContainer.getGroupFunctions(), defaultFunctions, allColumnNames
        );

        return composite;
    }

    @Override
    protected void okPressed() {
        List<SQLGroupingAttribute> attributes = StringEditorTableUtils.collectCustomValues(columnsTable);
        List<String> functions = StringEditorTableUtils.collectStringValues(functionsTable);
        resultsContainer.setGrouping(attributes, functions);
        super.okPressed();
    }

    @NotNull
    private String getAttributeBindingName(@NotNull DBDAttributeBinding binding) {
        if (binding instanceof DBDAttributeBindingMeta) {
            return DBUtils.getQuotedIdentifier(binding.getDataSource(), binding.getMetaAttribute().getLabel());
        } else {
            return binding.getFullyQualifiedName(DBPEvaluationContext.DML);
        }
    }

    @NotNull
    private Table createFunctionsTable(
        @NotNull Composite parent,
        @NotNull StringContentProposalProvider proposalProvider,
        @NotNull List<String> groupFunctions,
        @NotNull List<String> defaultFunctions,
        @NotNull List<String> columnNames
    ) {
        var tableFactory = new FunctionsTableFactory(parent, proposalProvider, groupFunctions, defaultFunctions, columnNames);
        return tableFactory.createTable();
    }

    @NotNull
    private Table createColumnsTable(
        @NotNull Composite parent,
        @NotNull StringContentProposalProvider proposalProvider,
        @NotNull List<SQLGroupingAttribute> groupAttributes,
        @NotNull List<String> allColumnNames
    ) {
        var tableFactory = new ColumnsTableFactory(parent, proposalProvider, groupAttributes, allColumnNames);
        return tableFactory.createTable();
    }

    private class GroupingAttributeValueManager implements StringEditorTableUtils.TableValuesManager<SQLGroupingAttribute> {
        @NotNull
        @Override
        public DBPImage getIcon(@Nullable SQLGroupingAttribute value) {
            if (value instanceof SQLGroupingAttribute.BoundAttribute bound) {
                DBSDataType type = bound.getBinding().getDataType();
                return type == null ? DBIcon.TYPE_UNKNOWN : DBValueFormatting.getTypeImage(type);
            } else {
                return DBIcon.TREE_ATTRIBUTE;
            }
        }

        @NotNull
        @Override
        public String getString(@Nullable SQLGroupingAttribute value) {
            return value == null ? "" : value.getDisplayName();
        }

        @NotNull
        @Override
        public Boolean isEditable(@Nullable SQLGroupingAttribute value) {
            return value == null || value instanceof SQLGroupingAttribute.CustomAttribute;
        }

        @Nullable
        @Override
        public SQLGroupingAttribute prepareNewValue(@Nullable SQLGroupingAttribute originalValue, @Nullable String string) {
            if (CommonUtils.isNotEmpty(string)) {
                return SQLGroupingAttribute.makeCustom(resultsContainer.getDataContainer().getDataSource(), string);
            } else {
                return null;
            }
        }
    }

    private class ColumnsTableFactory extends StringEditorTableFactory<SQLGroupingAttribute> {

        private final List<String> columnNames;

        ColumnsTableFactory(
            @NotNull Composite parent,
            @Nullable IContentProposalProvider proposalProvider,
            @NotNull List<SQLGroupingAttribute> values,
            @NotNull List<String> allColumnNames
        ) {
            super(
                UIUtils.createControlGroup(parent, ResultSetMessages.grouping_panel_column_panel_title, 2, GridData.FILL_BOTH, 0),
                values,
                new GroupingAttributeValueManager(),
                proposalProvider,
                true
            );
            this.columnNames = allColumnNames;
        }

        @NotNull
        @Override
        protected Control addButton(@NotNull Composite buttonsGroup) {
            Button addButton = new Button(buttonsGroup, SWT.PUSH | SWT.ARROW | SWT.DOWN);
            addButton.setText(UIMessages.button_add);
            addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Menu items = createAddMenu(addButton);
            addButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    items.setVisible(true);
                }
            });
            return addButton;
        }

        @NotNull
        private Menu createAddMenu(@NotNull Button addButton) {
            MenuManager rootManager = new MenuManager();
            addButton.addDisposeListener(e -> rootManager.dispose());
            addCustomColumn(rootManager);
            for (String columnName : columnNames) {
                rootManager.add(new Action(columnName) {
                                    @Override
                                    public void run() {
                                        TableItem newItem = new TableItem(valueTable, SWT.LEFT);
                                        newItem.setText(columnName);
                                        addTableItem(newItem);
                                    }
                                }
                );
            }
            return rootManager.createContextMenu(addButton);
        }

        private void addCustomColumn(@NotNull MenuManager addMenu) {
            addMenu.add(new Action(ResultSetMessages.grouping_panel_column_panel_custom_label) {
                @Override
                public void run() {
                    addTableItem(new TableItem(valueTable, SWT.LEFT));
                }
            });
        }
    }

    private class FunctionsTableFactory extends StringEditorTableFactory<String> {

        private final List<String> defaultFunctions;

        private final List<String> columns;


        FunctionsTableFactory(
            @NotNull Composite parent,
            @Nullable IContentProposalProvider proposalProvider,
            @NotNull List<String> values,
            @NotNull List<String> defaultFunctions,
            @NotNull List<String> columns
        ) {
            super(
                UIUtils.createControlGroup(parent, ResultSetMessages.grouping_panel_function_panel_title, 2, GridData.FILL_BOTH, 0),
                values,
                new StringEditorTableFactory.StringValuesManager(DBIcon.TREE_FUNCTION),
                proposalProvider,
                true
            );
            this.defaultFunctions = defaultFunctions;
            this.columns = new ArrayList<>(columns);
            this.columns.addFirst("*");
        }

        @NotNull
        @Override
        protected Control addButton(@NotNull Composite buttonsGroup) {
            Button addButton = new Button(buttonsGroup, SWT.PUSH | SWT.ARROW | SWT.DOWN);
            addButton.setText(UIMessages.button_add);
            addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Menu items = createAddMenu(addButton);
            addButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    items.setVisible(true);
                }
            });
            return addButton;
        }

        @NotNull
        private Menu createAddMenu(@NotNull Button addButton) {
            MenuManager rootManager = new MenuManager();
            addButton.addDisposeListener(e -> rootManager.dispose());
            addCustomFunction(rootManager);
            for (String function : defaultFunctions) {
                MenuManager functionMenu = new MenuManager(function);
                for (String column : columns) {
                    functionMenu.add(createSubmenuAction(function, column));
                }
                rootManager.add(functionMenu);
            }
            Menu addMenu = rootManager.createContextMenu(addButton);
            return addMenu;
        }

        private void addCustomFunction(@NotNull MenuManager addMenu) {
            addMenu.add(new Action(ResultSetMessages.grouping_panel_function_panel_custom_label) {
                @Override
                public void run() {
                    addTableItem(new TableItem(valueTable, SWT.LEFT));
                }
            });
        }

        @NotNull
        private Action createSubmenuAction(@NotNull String functionName, @NotNull String columnName) {
            return new Action(columnName) {
                @Override
                public void run() {
                    String functionCall = functionName + "(" + columnName + ")";
                    TableItem newItem = new TableItem(valueTable, SWT.LEFT);
                    newItem.setText(functionCall);
                    addTableItem(newItem);
                }
            };
        }
    }
}
