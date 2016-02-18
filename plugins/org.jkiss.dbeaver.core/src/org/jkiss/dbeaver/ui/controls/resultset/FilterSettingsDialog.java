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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTreeEditor;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;

class FilterSettingsDialog extends HelpEnabledDialog {

    private static final String DIALOG_ID = "DBeaver.FilterSettingsDialog";//$NON-NLS-1$

    public final Comparator<DBDAttributeBinding> POSITION_SORTER = new Comparator<DBDAttributeBinding>() {
        @Override
        public int compare(DBDAttributeBinding o1, DBDAttributeBinding o2) {
            final DBDAttributeConstraint c1 = getBindingConstraint(o1);
            final DBDAttributeConstraint c2 = getBindingConstraint(o2);
            return c1.getVisualPosition() - c2.getVisualPosition();
        }
    };

    private final ResultSetViewer resultSetViewer;
    private final List<DBDAttributeBinding> attributes;

    private CheckboxTreeViewer columnsViewer;
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
        this.attributes = Arrays.asList(resultSetViewer.getModel().getAttributes());
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

        TreeColumn criteriaColumn;
        {
            Composite columnsGroup = UIUtils.createPlaceholder(tabFolder, 1);

            columnsViewer = new CheckboxTreeViewer(columnsGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK);
            columnsViewer.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    final java.util.List<DBDAttributeBinding> nestedBindings = ((DBDAttributeBinding) parentElement).getNestedBindings();
                    if (nestedBindings == null || nestedBindings.isEmpty()) {
                        return null;
                    }
                    final DBDAttributeBinding[] res = nestedBindings.toArray(new DBDAttributeBinding[nestedBindings.size()]);
                    Arrays.sort(res, POSITION_SORTER);
                    return res;
                }

                @Override
                public boolean hasChildren(Object element) {
                    final java.util.List<DBDAttributeBinding> nestedBindings = ((DBDAttributeBinding) element).getNestedBindings();
                    return nestedBindings != null && !nestedBindings.isEmpty();
                }
            });
            columnsViewer.setLabelProvider(new ColumnLabelProvider());
            columnsViewer.setCheckStateProvider(new CheckStateProvider());
            final Tree columnsTree = columnsViewer.getTree();
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            //gd.heightHint = 300;
            columnsTree.setLayoutData(gd);
            columnsTree.setHeaderVisible(true);
            columnsTree.setLinesVisible(true);
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, CoreMessages.controls_resultset_filter_column_name);
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, "#");
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, CoreMessages.controls_resultset_filter_column_order);
            criteriaColumn = UIUtils.createTreeColumn(columnsTree, SWT.LEFT, CoreMessages.controls_resultset_filter_column_criteria);

            new CustomTreeEditor(columnsTree) {
                {
                    firstTraverseIndex = 3;
                    lastTraverseIndex = 3;
                }
                @Override
                protected Control createEditor(Tree table, int index, TreeItem item) {
                    if (index == 2) {
                        toggleColumnOrder(item);
                        return null;
                    } else if (index == 3 && resultSetViewer.supportsDataFilter()) {
                        Text text = new Text(columnsTree, SWT.BORDER);
                        text.setText(item.getText(index));
                        text.selectAll();
                        return text;
                    }
                    return null;
                }
                @Override
                protected void saveEditorValue(Control control, int index, TreeItem item) {
                    Text text = (Text) control;
                    String criteria = text.getText().trim();
                    DBDAttributeConstraint constraint = getBindingConstraint((DBDAttributeBinding) item.getData());
                    if (CommonUtils.isEmpty(criteria)) {
                        constraint.setCriteria(null);
                    } else {
                        constraint.setCriteria(criteria);
                    }
                    item.setText(3, criteria);
                }
                private void toggleColumnOrder(TreeItem item)
                {
                    DBDAttributeConstraint constraint = getBindingConstraint((DBDAttributeBinding) item.getData());
                    if (constraint.getOrderPosition() == 0) {
                        // Add new ordered column
                        constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
                        constraint.setOrderDescending(false);
                    } else if (!constraint.isOrderDescending()) {
                        constraint.setOrderDescending(true);
                    } else {
                        // Remove ordered column
/*
                        for (DBDAttributeConstraint con2 : dataFilter.getConstraints()) {
                            if (con2.getOrderPosition() > constraint.getOrderPosition()) {
                                con2.setOrderPosition(con2.getOrderPosition() - 1);
                            }
                        }
*/
                        constraint.setOrderPosition(0);
                        constraint.setOrderDescending(false);
                    }
                    columnsViewer.refresh();
                }
            };

            columnsViewer.addCheckStateListener(new ICheckStateListener() {
                @Override
                public void checkStateChanged(CheckStateChangedEvent event) {
                    DBDAttributeConstraint constraint = getBindingConstraint((DBDAttributeBinding) event.getElement());
                    constraint.setVisible(event.getChecked());
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
                        int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                        moveColumn(selectionIndex, selectionIndex - 1);
                    }
                });
                moveUpButton.setEnabled(false);
                final Button moveDownButton = UIUtils.createPushButton(controlGroup, "Move Down", null);
                moveDownButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        int selectionIndex = getSelectionIndex(columnsViewer.getTree());
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
                        refreshData();
                        //columnsViewer.refresh();
                        orderText.setText(""); //$NON-NLS-1$
                        whereText.setText(""); //$NON-NLS-1$
                    }
                });

                columnsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                        moveUpButton.setEnabled(selectionIndex > 0);
                        moveDownButton.setEnabled(selectionIndex >= 0 && selectionIndex < columnsViewer.getTree().getItemCount() - 1);
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
        columnsViewer.setInput(attributes);
        refreshData();

        // Pack UI
        UIUtils.packColumns(columnsViewer.getTree());
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

    private void refreshData() {
        Collections.sort(attributes, POSITION_SORTER);
        columnsViewer.refresh();
        columnsViewer.expandAll();
    }

    private int getSelectionIndex(Tree tree) {
        final TreeItem[] selection = tree.getSelection();
        if (selection.length == 0) {
            return 0;
        }
        return tree.indexOf(selection[0]);
    }

    private void moveColumn(int curIndex, int newIndex)
    {
        final DBDAttributeConstraint c1 = getBindingConstraint((DBDAttributeBinding) columnsViewer.getTree().getItem(curIndex).getData());
        final DBDAttributeConstraint c2 = getBindingConstraint((DBDAttributeBinding) columnsViewer.getTree().getItem(newIndex).getData());
        final int vp2 = c2.getVisualPosition();
        c2.setVisualPosition(c1.getVisualPosition());
        c1.setVisualPosition(vp2);
        refreshData();
        //columnsViewer.sh
//        DBDAttributeConstraint constraint = constraints.remove(curIndex);
//        constraints.add(newIndex, constraint);
//        columnsViewer.refresh();
//        columnsViewer.setSelection(columnsViewer.getSelection());
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
//            constraint.setVisualPosition(this.constraints.indexOf(constraint));
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
            !dataFilter.equals(resultSetViewer.getModel().getDataFilter()));
        super.okPressed();
    }

    class ColumnLabelProvider extends LabelProvider implements ITableLabelProvider
    {
        @Nullable
        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            DBDAttributeBinding binding = (DBDAttributeBinding) element;
            if (columnIndex == 0) {
                if (binding.getMetaAttribute() instanceof DBPImageProvider) {
                    return DBeaverIcons.getImage(((DBPImageProvider) binding.getMetaAttribute()).getObjectImage());
                } else {
                    return null;
                }
            }
            if (columnIndex == 2) {
                DBDAttributeConstraint constraint = getBindingConstraint(binding);
                if (constraint.getOrderPosition() > 0) {
                    return DBeaverIcons.getImage(constraint.isOrderDescending() ? UIIcon.SORT_DECREASE : UIIcon.SORT_INCREASE);
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            DBDAttributeBinding binding = (DBDAttributeBinding) element;
            DBDAttributeConstraint constraint = getBindingConstraint(binding);
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

    @NotNull
    private DBDAttributeConstraint getBindingConstraint(DBDAttributeBinding binding) {
        for (DBDAttributeConstraint constraint : constraints) {
            if (constraint.matches(binding, true)) {
                return constraint;
            }
        }
        throw new IllegalStateException("Can't find constraint for binding " + binding);
    }

    class CheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked(Object element)
        {
            return getBindingConstraint(((DBDAttributeBinding)element)).isVisible();
        }

        @Override
        public boolean isGrayed(Object element)
        {
            return false;
        }

    }

}
