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
package org.jkiss.dbeaver.ui.controls.resultset.valuefilter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDLabelValuePairExt;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.AbstractPopupPanel;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.text.NumberFormat;

public class FilterValueEditPopup extends AbstractPopupPanel {

    private static final String DIALOG_ID = "DBeaver.FilterValueEditMenu";//$NON-NLS-1$

    private static final String PROP_SHOW_ROW_COUNT = "showRowCount";
    private static final String PROP_QUERY_DATABASE = "queryDatabase";
    private static final String PROP_CASE_INSENSITIVE_SEARCH = "caseInsensitiveSearch";

    private Object value;
    private GenericFilterValueEdit filter;
    private Point location;
    private Button showRowCountCheck;

    public FilterValueEditPopup(Shell parentShell, @NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows) {
        super(parentShell, NLS.bind(ResultSetMessages.dialog_filter_value_edit_title, attr.getFullyQualifiedName(DBPEvaluationContext.UI)));
        setShellStyle(SWT.SHELL_TRIM);
        filter = new GenericFilterValueEdit(viewer, attr, rows, DBCLogicalOperator.IN);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        if (location != null) {
            return location;
        }
        return super.getInitialLocation(initialSize);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DBSEntityReferrer descReferrer = ResultSetUtils.getEnumerableConstraint(filter.getAttribute());

        Composite group = (Composite) super.createDialogArea(parent);
        {
            Composite labelComposite = UIUtils.createComposite(group, 2);
            labelComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label controlLabel = UIUtils.createControlLabel(labelComposite, ResultSetMessages.dialog_filter_value_edit_label_choose_values);
            controlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (descReferrer instanceof DBSEntityAssociation) {
                Link hintLabel = UIUtils.createLink(labelComposite, ResultSetMessages.dialog_filter_value_edit_label_define_description, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditDictionaryPage editDictionaryPage = new EditDictionaryPage(((DBSEntityAssociation) descReferrer).getAssociatedEntity());
                        if (editDictionaryPage.edit(parent.getShell())) {
                            reloadFilterValues();
                        }
                    }
                });
                hintLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            } else {
                UIUtils.createEmptyLabel(labelComposite, 1, 1);
            }
        }

        Text filterTextbox = filter.addFilterText(group);
        filterTextbox.setFocus();
        filterTextbox.addTraverseListener(e -> {
            Table table = filter.getTableViewer().getTable();
            if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS || e.detail == SWT.TRAVERSE_ARROW_NEXT) {
                if (table.getSelectionIndex() < 0 && table.getItemCount() > 0) {
                    table.setSelection(0);
                }
                table.setFocus();
            } else if (e.detail == SWT.TRAVERSE_RETURN) {
                applyFilterValue();
            }
        });
        UIUtils.addEmptyTextHint(filterTextbox, text -> ResultSetMessages.dialog_filter_value_edit_text_hint);

        Composite tableComposite = UIUtils.createComposite(group, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        tableComposite.setLayoutData(gd);

        filter.setupTable(
            tableComposite,
            SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL |
                (filter.getOperator() == DBCLogicalOperator.IN ? SWT.CHECK : SWT.NONE),
            true,
            true,
            new GridData(GridData.FILL_BOTH));

        Table table = filter.getTableViewer().getTable();

        ViewerColumnController<?, ?> columnController = new ViewerColumnController<>("sqlFilterValueEditPopup", filter.getTableViewer());
        columnController.addColumn(ResultSetMessages.dialog_filter_value_edit_table_value_label, ResultSetMessages.dialog_filter_value_edit_table_value_description, SWT.LEFT, true, true, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return filter.getAttribute().getValueHandler().getValueDisplayString(filter.getAttribute(), ((DBDLabelValuePair) element).getValue(), DBDDisplayFormat.UI);
            }
        });
        if (descReferrer != null) {
            columnController.addColumn(ResultSetMessages.dialog_filter_value_edit_table_description_label, ResultSetMessages.dialog_filter_value_edit_table_description_description, SWT.LEFT, true, true, new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBDLabelValuePair) element).getLabel();
                }
            });
        }
        if (descReferrer == null) {
            columnController.addColumn(ResultSetMessages.dialog_filter_value_edit_table_count_label, ResultSetMessages.dialog_filter_value_edit_table_count_description, SWT.LEFT, true, true, true, null, new ColumnLabelProvider() {
                private final NumberFormat numberFormat = NumberFormat.getInstance();

                @Override
                public String getText(Object element) {
                    if (element instanceof DBDLabelValuePairExt) {
                        return numberFormat.format(((DBDLabelValuePairExt) element).getCount());
                    } else {
                        return CommonUtils.notEmpty(((DBDLabelValuePair) element).getLabel());
                    }
                }
            }, null);
        }
        columnController.createColumns(true);

        filter.getTableViewer().addDoubleClickListener(event -> {
            value = filter.getSelectedFilterValue();
            close();
        });

        final Group optionsGroup = UIUtils.createControlGroup(tableComposite, ResultSetMessages.dialog_filter_value_edit_table_group_options, 0, GridData.FILL_HORIZONTAL, 0);
        optionsGroup.moveAbove(filter.getButtonsPanel());
        {
            if (isAttributeSupportsLike()) {
                final Button caseInsensitiveSearchCheck = UIUtils.createCheckbox(
                    optionsGroup,
                    ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_case_insensitive_label,
                    ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_case_insensitive_description,
                    isCaseInsensitiveSearchEnabled(),
                    1
                );
                caseInsensitiveSearchCheck.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        getDialogBoundsSettings().put(PROP_CASE_INSENSITIVE_SEARCH, caseInsensitiveSearchCheck.getSelection());
                        reloadFilterValues();
                    }
                });
                caseInsensitiveSearchCheck.setEnabled(isQueryDatabaseEnabled());
                ((GridLayout) optionsGroup.getLayout()).numColumns++;
            }
            Button queryDatabaseCheck = UIUtils.createCheckbox(
                optionsGroup,
                ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_read_from_server_label,
                ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_read_from_server_description,
                isQueryDatabaseEnabled(),
                1);
            ((GridLayout) optionsGroup.getLayout()).numColumns++;
            queryDatabaseCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean isEnabled = queryDatabaseCheck.getSelection();
                    getDialogBoundsSettings().put(PROP_QUERY_DATABASE, isEnabled);
                    if (showRowCountCheck != null) {
                        showRowCountCheck.setEnabled(isEnabled);
                    }
                    reloadFilterValues();
                }
            });
            closeOnFocusLost(queryDatabaseCheck);
        }
        if (!filter.isDictionarySelector()) {
            showRowCountCheck = UIUtils.createCheckbox(
                optionsGroup,
                ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_show_row_count_label,
                ResultSetMessages.dialog_filter_value_edit_table_options_checkbox_show_row_count_description,
                isRowCountEnabled(),
                1);
            ((GridLayout) optionsGroup.getLayout()).numColumns++;
            showRowCountCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getDialogBoundsSettings().put(PROP_SHOW_ROW_COUNT, showRowCountCheck.getSelection());
                    reloadFilterValues();
                }
            });
            showRowCountCheck.setEnabled(isQueryDatabaseEnabled());
            closeOnFocusLost(showRowCountCheck);
        }

        filter.createFilterButton(ResultSetMessages.sql_editor_resultset_filter_panel_btn_apply, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                applyFilterValue();
            }
        });

        closeOnFocusLost(filterTextbox, table);

        filter.setFilterPattern(null);
        reloadFilterValues();

        return tableComposite;
    }

    private boolean isRowCountEnabled() {
        return getDialogBoundsSettings().getBoolean(PROP_SHOW_ROW_COUNT);
    }

    private boolean isQueryDatabaseEnabled() {
        return CommonUtils.getBoolean(getDialogBoundsSettings().get(PROP_QUERY_DATABASE), true);
    }

    private boolean isCaseInsensitiveSearchEnabled() {
        return CommonUtils.getBoolean(getDialogBoundsSettings().getBoolean(PROP_CASE_INSENSITIVE_SEARCH), true);
    }

    private boolean isAttributeSupportsLike() {
        final DBDAttributeBinding attribute = filter.getAttribute();
        return ArrayUtils.contains(DBUtils.getAttributeOperators(attribute), DBCLogicalOperator.LIKE)
            && SQLUtils.getDialectFromObject(attribute).getCaseInsensitiveExpressionFormatter(DBCLogicalOperator.LIKE) != null;
    }

    private void reloadFilterValues() {
        filter.setQueryDatabase(isQueryDatabaseEnabled());
        filter.setShowRowCount(isRowCountEnabled());
        filter.setCaseInsensitiveSearch(isCaseInsensitiveSearchEnabled());
        filter.loadValues(() ->
            UIUtils.asyncExec(() -> {
                Table table = filter.getTableViewer().getTable();
                if (table != null && !table.isDisposed()) {
                    UIUtils.packColumns(table, false);
                }
            }));
    }

    private void applyFilterValue() {
        value = filter.getFilterValue();
        okPressed();
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        return UIUtils.createPlaceholder(parent, 1);
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Object getValue() {
        return value;
    }
}
