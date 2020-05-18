/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.dialogs.FilteredTree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBValueFormatting;
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
import org.jkiss.dbeaver.ui.controls.NamedObjectPatternFilter;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

class FilterSettingsDialog extends HelpEnabledDialog {

    private static final String DIALOG_ID = "DBeaver.FilterSettingsDialog";//$NON-NLS-1$

    private final Comparator<DBDAttributeBinding> POSITION_SORTER = (o1, o2) -> {
        final DBDAttributeConstraint c1 = getBindingConstraint(o1);
        final DBDAttributeConstraint c2 = getBindingConstraint(o2);
        return c1.getVisualPosition() - c2.getVisualPosition();
    };
    private final Comparator<DBDAttributeBinding> ALPHA_SORTER = Comparator.comparing(DBDAttributeBinding::getName);

    private final ResultSetViewer resultSetViewer;
    private final List<DBDAttributeBinding> attributes;

    private CheckboxTreeViewer columnsViewer;
    private DBDDataFilter dataFilter;
    private Text whereText;
    private Text orderText;
    // Keep constraints in a copy because we use this list as table viewer model
    private java.util.List<DBDAttributeConstraint> constraints;
    private ToolItem moveTopButton;
    private ToolItem moveUpButton;
    private ToolItem moveDownButton;
    private ToolItem moveBottomButton;
    private Comparator<DBDAttributeBinding> activeSorter = POSITION_SORTER;

    FilterSettingsDialog(ResultSetViewer resultSetViewer)
    {
        super(resultSetViewer.getControl().getShell(), IHelpContextIds.CTX_DATA_FILTER);
        this.resultSetViewer = resultSetViewer;
        this.dataFilter = new DBDDataFilter(resultSetViewer.getModel().getDataFilter());
        this.constraints = new ArrayList<>(dataFilter.getConstraints());

        DBDAttributeBinding[] modelAttrs = resultSetViewer.getModel().getAttributes();
        this.attributes = new ArrayList<>(modelAttrs.length);
        Collections.addAll(this.attributes, modelAttrs);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(ResultSetMessages.controls_resultset_filter_title);
        getShell().setImage(DBeaverIcons.getImage(UIIcon.FILTER));

        Composite composite = (Composite) super.createDialogArea(parent);

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        TreeColumn criteriaColumn;
        {
            Composite columnsGroup = UIUtils.createPlaceholder(tabFolder, 1);

            FilteredTree filteredTree = new FilteredTree(columnsGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK, new NamedObjectPatternFilter(), true) {
                @Override
                protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
                    columnsViewer = new CheckboxTreeViewer(parent, style);
                    return columnsViewer;
                }
            };

            columnsViewer.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    final java.util.List<DBDAttributeBinding> nestedBindings = ((DBDAttributeBinding) parentElement).getNestedBindings();
                    if (nestedBindings == null || nestedBindings.isEmpty()) {
                        return null;
                    }
                    final DBDAttributeBinding[] res = nestedBindings.toArray(new DBDAttributeBinding[0]);
                    Arrays.sort(res, activeSorter);
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
            columnsTree.setLayoutData(gd);
            columnsTree.setHeaderVisible(true);
            columnsTree.setLinesVisible(true);
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, ResultSetMessages.controls_resultset_filter_column_name);
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, "#");
            UIUtils.createTreeColumn(columnsTree, SWT.LEFT, ResultSetMessages.controls_resultset_filter_column_order);
            criteriaColumn = UIUtils.createTreeColumn(columnsTree, SWT.LEFT, ResultSetMessages.controls_resultset_filter_column_criteria);

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

            columnsViewer.addCheckStateListener(event -> {
                DBDAttributeConstraint constraint = getBindingConstraint((DBDAttributeBinding) event.getElement());
                constraint.setVisible(event.getChecked());
            });

            {
                ToolBar toolbar = new ToolBar(columnsGroup, SWT.HORIZONTAL | SWT.RIGHT);
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.verticalIndent = 3;
                toolbar.setLayoutData(gd);
                toolbar.setLayout(new FillLayout());
                moveTopButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_move_to_top, UIIcon.ARROW_TOP, () -> {
                    int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                    moveColumns(selectionIndex, 0);
                });
                moveTopButton.setEnabled(false);
                moveUpButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_move_up, UIIcon.ARROW_UP, () -> {
                    int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                    swapColumns(selectionIndex, selectionIndex - 1);
                });
                moveUpButton.setEnabled(false);
                moveDownButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_move_down, UIIcon.ARROW_DOWN, () -> {
                    int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                    swapColumns(selectionIndex, selectionIndex + 1);
                });
                moveDownButton.setEnabled(false);
                moveBottomButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_move_to_bottom, UIIcon.ARROW_BOTTOM, () -> {
                    int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                    moveColumns(selectionIndex, getItemsCount() - 1);
                });
                moveBottomButton.setEnabled(false);
                UIUtils.createToolBarSeparator(toolbar, SWT.VERTICAL);
                createToolItem(toolbar, ResultSetMessages.dialog_toolbar_sort, UIIcon.SORT, () -> {
                    attributes.sort(ALPHA_SORTER);
                    for (int i = 0; i < attributes.size(); i++) {
                        final DBDAttributeConstraint constraint = getBindingConstraint(attributes.get(i));
                        constraint.setVisualPosition(i);
                    }
                    columnsViewer.refresh();
                });
                UIUtils.createToolBarSeparator(toolbar, SWT.VERTICAL);
                ToolItem showAllButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_show_all, null, () -> {
                    for (DBDAttributeConstraint constraint : constraints) {
                        constraint.setVisible(true);
                    }
                    columnsViewer.refresh();
                });
                showAllButton.setImage(UIUtils.getShardImage(ISharedImages.IMG_ETOOL_DEF_PERSPECTIVE));
                ToolItem showNoneButton = createToolItem(toolbar, ResultSetMessages.dialog_toolbar_show_none, null, () -> {
                    for (DBDAttributeConstraint constraint : constraints) {
                        constraint.setVisible(false);
                    }
                    columnsViewer.refresh();
                });
                showNoneButton.setImage(UIUtils.getShardImage(ISharedImages.IMG_ELCL_REMOVEALL));
                createToolItem(toolbar, ResultSetMessages.dialog_toolbar_reset, UIIcon.REFRESH, () -> {
                    dataFilter.reset();
                    constraints = new ArrayList<>(dataFilter.getConstraints());
                    refreshData();
                    //columnsViewer.refresh();
                    orderText.setText(""); //$NON-NLS-1$
                    whereText.setText(""); //$NON-NLS-1$
                });

                columnsViewer.addSelectionChangedListener(event -> {
                    int selectionIndex = getSelectionIndex(columnsViewer.getTree());
                    moveTopButton.setEnabled(selectionIndex > 0);
                    moveUpButton.setEnabled(selectionIndex > 0);
                    moveDownButton.setEnabled(selectionIndex >= 0 && selectionIndex < getItemsCount() - 1);
                    moveBottomButton.setEnabled(selectionIndex >= 0 && selectionIndex < getItemsCount() - 1);
                });

            }
            TabItem libsTab = new TabItem(tabFolder, SWT.NONE);
            libsTab.setText(ResultSetMessages.controls_resultset_filter_group_columns);
            libsTab.setToolTipText("Set criteria and order for individual column(s)");
            libsTab.setControl(columnsGroup);
        }

        createCustomFilters(tabFolder);

        // Fill columns
        columnsViewer.setInput(attributes);
        refreshData();

        // Pack UI
        UIUtils.asyncExec(() -> UIUtils.packColumns(columnsViewer.getTree(), true, new float[] { .45f, .05f, .05f, .45f}));
        //UIUtils.packColumns(filterViewer.getTable());

        if (criteriaColumn.getWidth() < 200) {
            criteriaColumn.setWidth(200);
        }

        if (!resultSetViewer.supportsDataFilter()) {
            Label warnLabel = new Label(composite, SWT.NONE);
            warnLabel.setText(ResultSetMessages.controls_resultset_filter_warning_custom_order_disabled);
            warnLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        }


        return parent;
    }

    private int getItemsCount() {
        return columnsViewer.getTree().getItemCount();
    }

    private void refreshData() {
        attributes.sort(activeSorter);
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

    private void swapColumns(int curIndex, int newIndex)
    {
        final DBDAttributeConstraint c1 = getBindingConstraint((DBDAttributeBinding) columnsViewer.getTree().getItem(curIndex).getData());
        final DBDAttributeConstraint c2 = getBindingConstraint((DBDAttributeBinding) columnsViewer.getTree().getItem(newIndex).getData());
        final int vp2 = c2.getVisualPosition();
        c2.setVisualPosition(c1.getVisualPosition());
        c1.setVisualPosition(vp2);
        refreshData();
        moveTopButton.setEnabled(newIndex > 0);
        moveUpButton.setEnabled(newIndex > 0);
        moveDownButton.setEnabled(newIndex < getItemsCount() - 1);
        moveBottomButton.setEnabled(newIndex < getItemsCount() - 1);
    }

    private void moveColumns(int curIndex, int newIndex)
    {
        if (curIndex == newIndex) {
            return;
        }
        final DBDAttributeConstraint curAttr = getBindingConstraint((DBDAttributeBinding) columnsViewer.getTree().getItem(curIndex).getData());
        // Update other constraints indexes
        for (DBDAttributeConstraint c : constraints) {
            if (newIndex < curIndex) {
                if (c.getVisualPosition() >= newIndex && c.getVisualPosition() < curIndex) {
                    c.setVisualPosition(c.getVisualPosition() + 1);
                }
            } else {
                if (c.getVisualPosition() > curIndex && c.getVisualPosition() <= newIndex) {
                    c.setVisualPosition(c.getVisualPosition() - 1);
                }
            }
        }
        curAttr.setVisualPosition(newIndex);
        refreshData();
        moveTopButton.setEnabled(newIndex > 0);
        moveUpButton.setEnabled(newIndex > 0);
        moveDownButton.setEnabled(newIndex < getItemsCount() - 1);
        moveBottomButton.setEnabled(newIndex < getItemsCount() - 1);
    }

    private void createCustomFilters(TabFolder tabFolder)
    {
        Composite filterGroup = new Composite(tabFolder, SWT.NONE);
        filterGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        filterGroup.setLayout(new GridLayout(1, false));

        UIUtils.createControlLabel(filterGroup, ResultSetMessages.controls_resultset_filter_label_where);
        whereText = new Text(filterGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        whereText.setLayoutData(new GridData(GridData.FILL_BOTH));
        if (dataFilter.getWhere() != null) {
            whereText.setText(dataFilter.getWhere());
        }

        UIUtils.createControlLabel(filterGroup, ResultSetMessages.controls_resultset_filter_label_orderby);
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
        libsTab.setText(ResultSetMessages.controls_resultset_filter_group_custom);
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

        boolean filtersChanged = true;
        if (dataFilter.equalFilters(resultSetViewer.getModel().getDataFilter(), true)) {
            // Only attribute visibility was changed
            filtersChanged = false;
        }
        resultSetViewer.setDataFilter(
            dataFilter,
            filtersChanged);
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
                return DBeaverIcons.getImage(
                    DBValueFormatting.getObjectImage(binding.getMetaAttribute()));
            }
            if (columnIndex == 2) {
                DBDAttributeConstraint constraint = getBindingConstraint(binding);
                if (constraint.getOrderPosition() > 0) {
                    return DBeaverIcons.getImage(constraint.isOrderDescending() ? UIIcon.SORT_INCREASE : UIIcon.SORT_DECREASE);
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
                        return " " + String.valueOf(orderPosition);
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

    private static ToolItem createToolItem(ToolBar toolBar, String text, DBIcon icon, final Runnable action)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        if (icon != null) {
            item.setImage(DBeaverIcons.getImage(icon));
        }
        if (text != null) {
            //item.setText(text);
            item.setToolTipText(text);
        }
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                action.run();
            }
        });
        return item;
    }

}
