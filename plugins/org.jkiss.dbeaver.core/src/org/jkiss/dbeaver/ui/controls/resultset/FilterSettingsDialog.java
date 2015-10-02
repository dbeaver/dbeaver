/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTableEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class FilterSettingsDialog extends HelpEnabledDialog {

    private static final String DIALOG_ID = "DBeaver.FilterSettingsDialog";//$NON-NLS-1$

    private final ResultSetViewer resultSetViewer;

    private CheckboxTableViewer columnsViewer;
    private DBDDataFilter dataFilter;
    private Text whereText;
    private Text orderText;
    // Keep constraints in a copy because we use this list as table viewer model
    private java.util.List<DBDAttributeConstraint> constraints;

    public FilterSettingsDialog(ResultSetViewer resultSetViewer)
    {
        super(resultSetViewer.getControl().getShell(), IHelpContextIds.CTX_DATA_FILTER);
        this.resultSetViewer = resultSetViewer;
        this.dataFilter = new DBDDataFilter(resultSetViewer.getModel().getDataFilter());
        this.constraints = new ArrayList<>(dataFilter.getConstraints());
        Collections.sort(this.constraints, new Comparator<DBDAttributeConstraint>() {
            @Override
            public int compare(DBDAttributeConstraint o1, DBDAttributeConstraint o2)
            {
                return o1.getVisualPosition() - o2.getVisualPosition();
            }
        });
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(CoreMessages.controls_resultset_filter_title);
        getShell().setImage(DBeaverIcons.getImage(UIIcon.FILTER));

        Composite composite = (Composite) super.createDialogArea(parent);

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TableColumn criteriaColumn;
        {
            Composite columnsGroup = UIUtils.createPlaceholder(tabFolder, 1);

            columnsViewer = CheckboxTableViewer.newCheckList(columnsGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
            columnsViewer.setContentProvider(new ListContentProvider());
            columnsViewer.setLabelProvider(new ColumnLabelProvider());
            columnsViewer.setCheckStateProvider(new CheckStateProvider());
            final Table columnsTable = columnsViewer.getTable();
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            //gd.heightHint = 300;
            columnsTable.setLayoutData(gd);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_name);
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, "#");
            UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_order);
            criteriaColumn = UIUtils.createTableColumn(columnsTable, SWT.LEFT, CoreMessages.controls_resultset_filter_column_criteria);

            new CustomTableEditor(columnsTable) {
                {
                    firstTraverseIndex = 3;
                    lastTraverseIndex = 3;
                }
                @Override
                protected Control createEditor(Table table, int index, TableItem item) {
                    if (index == 2) {
                        toggleColumnOrder(item);
                        return null;
                    } else if (index == 3 && resultSetViewer.supportsDataFilter()) {
                        Text text = new Text(columnsTable, SWT.BORDER);
                        text.setText(item.getText(index));
                        text.selectAll();
                        return text;
                    }
                    return null;
                }
                @Override
                protected void saveEditorValue(Control control, int index, TableItem item) {
                    Text text = (Text) control;
                    String criteria = text.getText().trim();
                    DBDAttributeConstraint constraint = (DBDAttributeConstraint) item.getData();
                    if (CommonUtils.isEmpty(criteria)) {
                        constraint.setCriteria(null);
                    } else {
                        constraint.setCriteria(criteria);
                    }
                    item.setText(3, criteria);
                }
                private void toggleColumnOrder(TableItem item)
                {
                    DBDAttributeConstraint constraint = (DBDAttributeConstraint) item.getData();
                    if (constraint.getOrderPosition() == 0) {
                        // Add new ordered column
                        constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
                        constraint.setOrderDescending(false);
                    } else if (!constraint.isOrderDescending()) {
                        constraint.setOrderDescending(true);
                    } else {
                        // Remove ordered column
                        for (DBDAttributeConstraint con2 : dataFilter.getConstraints()) {
                            if (con2.getOrderPosition() > constraint.getOrderPosition()) {
                                con2.setOrderPosition(con2.getOrderPosition() - 1);
                            }
                        }
                        constraint.setOrderPosition(0);
                        constraint.setOrderDescending(false);
                    }
                    columnsViewer.refresh();
                }
            };

            columnsViewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    ((DBDAttributeConstraint) event.getElement()).setVisible(event.getChecked());
                }
            });

            {
                Composite controlGroup = new Composite(columnsGroup, SWT.NONE);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.verticalIndent = 3;
                controlGroup.setLayoutData(gd);
                controlGroup.setLayout(new FillLayout());
                final Button moveUpButton = UIUtils.createPushButton(controlGroup, "Move Up", null);
                moveUpButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        int selectionIndex = columnsViewer.getTable().getSelectionIndex();
                        moveColumn(selectionIndex, selectionIndex - 1);
                    }
                });
                moveUpButton.setEnabled(false);
                final Button moveDownButton = UIUtils.createPushButton(controlGroup, "Move Down", null);
                moveDownButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        int selectionIndex = columnsViewer.getTable().getSelectionIndex();
                        moveColumn(selectionIndex, selectionIndex + 1);
                    }
                });
                moveDownButton.setEnabled(false);
                Button showAllButton = UIUtils.createPushButton(controlGroup, "Show All", null);
                showAllButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        for (DBDAttributeConstraint constraint : constraints) {
                            constraint.setVisible(true);
                        }
                        columnsViewer.refresh();
                    }
                });
                Button showNoneButton = UIUtils.createPushButton(controlGroup, "Show None", null);
                showNoneButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        for (DBDAttributeConstraint constraint : constraints) {
                            constraint.setVisible(false);
                        }
                        columnsViewer.refresh();
                    }
                });
                Button resetButton = UIUtils.createPushButton(controlGroup, CoreMessages.controls_resultset_filter_button_reset, null);
                resetButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        dataFilter.reset();
                        constraints = new ArrayList<>(dataFilter.getConstraints());
                        columnsViewer.setInput(constraints);
                        //columnsViewer.refresh();
                        orderText.setText(""); //$NON-NLS-1$
                        whereText.setText(""); //$NON-NLS-1$
                    }
                });

                columnsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event)
                    {
                        int selectionIndex = columnsViewer.getTable().getSelectionIndex();
                        moveUpButton.setEnabled(selectionIndex > 0);
                        moveDownButton.setEnabled(selectionIndex >= 0 && selectionIndex < columnsViewer.getTable().getItemCount() - 1);
                    }
                });

            }
            TabItem libsTab = new TabItem(tabFolder, SWT.NONE);
            libsTab.setText(CoreMessages.controls_resultset_filter_group_columns);
            libsTab.setToolTipText("Set criteria and order for individual column(s)");
            libsTab.setControl(columnsGroup);
        }

        createCustomFilters(tabFolder);

        // Fill columns
        columnsViewer.setInput(constraints);

        // Pack UI
        UIUtils.packColumns(columnsViewer.getTable());
        //UIUtils.packColumns(filterViewer.getTable());

        if (criteriaColumn.getWidth() < 200) {
            criteriaColumn.setWidth(200);
        }

        if (!resultSetViewer.supportsDataFilter()) {
            Label warnLabel = new Label(composite, SWT.NONE);
            warnLabel.setText(CoreMessages.controls_resultset_filter_warning_custom_order_disabled);
            warnLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        }


        return parent;
    }

    private void moveColumn(int curIndex, int newIndex)
    {
        DBDAttributeConstraint constraint = constraints.remove(curIndex);
        constraints.add(newIndex, constraint);
        columnsViewer.refresh();
        columnsViewer.setSelection(columnsViewer.getSelection());
    }

    private void createCustomFilters(TabFolder tabFolder)
    {
        Composite filterGroup = new Composite(tabFolder, SWT.NONE);
        filterGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        filterGroup.setLayout(new GridLayout(1, false));

        UIUtils.createControlLabel(filterGroup, CoreMessages.controls_resultset_filter_label_where);
        whereText = new Text(filterGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        whereText.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (dataFilter.getWhere() != null) {
            whereText.setText(dataFilter.getWhere());
        }

        UIUtils.createControlLabel(filterGroup, CoreMessages.controls_resultset_filter_label_orderby);
        orderText = new Text(filterGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        orderText.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (dataFilter.getOrder() != null) {
            orderText.setText(dataFilter.getOrder());
        }

        if (!resultSetViewer.supportsDataFilter()) {
            filterGroup.setEnabled(false);
            ControlEnableState.disable(filterGroup);
        }

        TabItem libsTab = new TabItem(tabFolder, SWT.NONE);
        libsTab.setText(CoreMessages.controls_resultset_filter_group_custom);
        libsTab.setToolTipText("Set custom criteria and order for whole query");
        libsTab.setControl(filterGroup);
    }

    @Override
    public int open()
    {
        return super.open();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        //createButton(parent, IDialogConstants.ABORT_ID, CoreMessages.controls_resultset_filter_button_reset, false);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        super.buttonPressed(buttonId);
    }

    @Override
    protected void okPressed()
    {
        boolean hasVisibleColumns = false;
        for (DBDAttributeConstraint constraint : dataFilter.getConstraints()) {
            // Set correct visible position
            constraint.setVisualPosition(this.constraints.indexOf(constraint));
            if (constraint.isVisible()) {
                hasVisibleColumns = true;
            }
        }
        if (!hasVisibleColumns) {
            UIUtils.showMessageBox(getShell(), "Bad filter", "You have to set at least one column visible", SWT.ICON_WARNING);
            return;
        }
        if (!CommonUtils.isEmpty(orderText.getText())) {
            dataFilter.setOrder(orderText.getText());
        } else {
            dataFilter.setOrder(null);
        }
        if (!CommonUtils.isEmpty(whereText.getText())) {
            dataFilter.setWhere(whereText.getText());
        } else {
            dataFilter.setWhere(null);
        }
        resultSetViewer.setDataFilter(
            dataFilter,
            !dataFilter.equalFilters(resultSetViewer.getModel().getDataFilter()));
        super.okPressed();
    }

    class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Nullable
        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            DBDAttributeConstraint constraint = (DBDAttributeConstraint) element;
            if (columnIndex == 0 && constraint.getAttribute() instanceof DBPImageProvider) {
                return DBeaverIcons.getImage(((DBPImageProvider) constraint.getAttribute()).getObjectImage());
            }
            if (columnIndex == 2) {
                if (constraint.getOrderPosition() > 0) {
                    return DBeaverIcons.getImage(constraint.isOrderDescending() ? UIIcon.SORT_DECREASE : UIIcon.SORT_INCREASE);
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            DBDAttributeConstraint constraint = (DBDAttributeConstraint) element;
            switch (columnIndex) {
                case 0: return constraint.getAttribute().getName();
                case 1: return String.valueOf(constraint.getOriginalVisualPosition() + 1);
                case 2: {
                    int orderPosition = constraint.getOrderPosition();
                    if (orderPosition > 0) {
                        return String.valueOf(orderPosition);
                    }
                    return ""; //$NON-NLS-1$
                }
                case 3: {
                    DBCExecutionContext executionContext = resultSetViewer.getExecutionContext();
                    if (executionContext != null) {
                        String condition = SQLUtils.getConstraintCondition(executionContext.getDataSource(), constraint, true);
                        if (condition != null) {
                            return condition;
                        }
                    }
                    return ""; //$NON-NLS-1$
                }
                default: return ""; //$NON-NLS-1$
            }
        }

    }

    class CheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object element)
        {
            return ((DBDAttributeConstraint)element).isVisible();
        }

        @Override
        public boolean isGrayed(Object element)
        {
            return false;
        }

    }

}
