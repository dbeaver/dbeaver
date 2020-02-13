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
package org.jkiss.dbeaver.ui.controls.resultset.valuefilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;


class GenericFilterValueEdit {

    private static final Log log = Log.getLog(GenericFilterValueEdit.class);

    private TableViewer tableViewer;
    private String filterPattern;

    private KeyLoadJob loadJob;
    private IValueEditor editor;
    private Text textControl;

    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final DBDAttributeBinding attribute;
    @NotNull
    private final ResultSetRow[] rows;
    @NotNull
    private final DBCLogicalOperator operator;

    private boolean isCheckedTable;

    private static final int MAX_MULTI_VALUES = 1000;
    private static final String MULTI_KEY_LABEL = "...";
    private Composite buttonsPanel;
    private Button toggleButton;

    private transient final Set<Object> savedValues = new HashSet<>();

    GenericFilterValueEdit(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull ResultSetRow[] rows, @NotNull DBCLogicalOperator operator) {
        this.viewer = viewer;
        this.attribute = attribute;
        this.rows = rows;
        this.operator = operator;
    }

    @NotNull
    public ResultSetViewer getViewer() {
        return viewer;
    }

    public TableViewer getTableViewer() {
        return tableViewer;
    }

    public String getFilterPattern() {
        return filterPattern;
    }

    public void setFilterPattern(String filterPattern) {
        this.filterPattern = filterPattern;
    }

    @NotNull
    public DBDAttributeBinding getAttribute() {
        return attribute;
    }

    @NotNull
    public ResultSetRow[] getRows() {
        return rows;
    }

    @NotNull
    public DBCLogicalOperator getOperator() {
        return operator;
    }

    public IValueEditor getEditor() {
        return editor;
    }

    public void setEditor(IValueEditor editor) {
        this.editor = editor;
    }

    void setupTable(Composite composite, int style, boolean visibleLines, boolean visibleHeader, Object layoutData) {

        tableViewer = new TableViewer(composite, style);
        Table table = this.tableViewer.getTable();
        table.setLinesVisible(false);
        table.setHeaderVisible(visibleHeader);
        table.setLayoutData(layoutData);
        this.tableViewer.setContentProvider(new ListContentProvider());

        isCheckedTable = (style & SWT.CHECK) == SWT.CHECK;

        if (isCheckedTable) {
            buttonsPanel = UIUtils.createComposite(composite, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            toggleButton = UIUtils.createDialogButton(buttonsPanel, "&Select All", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    TableItem[] items = tableViewer.getTable().getItems();
                    if (Boolean.FALSE.equals(toggleButton.getData())) {
                        // Clear all checked
                        for (TableItem item : items) {
                            item.setChecked(false);
                        }
                        toggleButton.setData(false);
                        savedValues.clear();
                    } else {
                        for (TableItem item : items) {
                            item.setChecked(true);
                            savedValues.add((((DBDLabelValuePair) item.getData())).getValue());
                        }
                        toggleButton.setData(true);
                    }
                    updateToggleButton(toggleButton);
                }
            });
            toggleButton.setData(true);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 120;
            toggleButton.setLayoutData(gd);
            UIUtils.createEmptyLabel(buttonsPanel, 1, 1).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            tableViewer.getTable().addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e.detail == SWT.CHECK) {
                        DBDLabelValuePair value = (DBDLabelValuePair) e.item.getData();
                        if (((TableItem)e.item).getChecked()) {
                            savedValues.add(value.getValue());
                        } else {
                            savedValues.remove(value.getValue());
                        }
                        updateToggleButton(toggleButton);
                    }
                }
            });
        }
    }

    private void updateToggleButton(Button toggleButton) {
        boolean hasCheckedItems = hasCheckedItems();
        toggleButton.setText(hasCheckedItems ? "&Clear All" : "&Select All");
        toggleButton.setData(!hasCheckedItems);
    }

    private boolean hasCheckedItems() {
        for (TableItem items : tableViewer.getTable().getItems()) {
            if (items.getChecked()) return true;
        }
        return false;
    }

    void addContextMenu(Action[] actions) {
        UIUtils.createTableContextMenu(tableViewer.getTable(), menu -> {
            for (Action act : actions) {
                menu.add(act);
            }
            menu.add(new Separator());
            return true;
        });
    }

    Collection<DBDLabelValuePair> getMultiValues() {
        return (Collection<DBDLabelValuePair>) tableViewer.getInput();
    }

    Text addFilterTextbox(Composite composite) {

        // Create filter text
        final Text valueFilterText = new Text(composite, SWT.BORDER);
        valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        valueFilterText.addModifyListener(e -> {
            filterPattern = valueFilterText.getText();
            if (filterPattern.isEmpty()) {
                filterPattern = null;
            }
            loadValues(null);
        });
        return valueFilterText;
    }

    void loadValues(Runnable onFinish) {
        if (loadJob != null) {
            loadJob.schedule(200);
            return;
        }
        // Load values
        final DBSEntityReferrer enumerableConstraint = ResultSetUtils.getEnumerableConstraint(attribute);
        if (enumerableConstraint != null) {
            loadConstraintEnum(enumerableConstraint, onFinish);
        } else if (attribute.getEntityAttribute() instanceof DBSAttributeEnumerable) {
            loadAttributeEnum((DBSAttributeEnumerable) attribute.getEntityAttribute(), onFinish);
        } else {
            loadMultiValueList(Collections.emptyList(), true);
        }
    }

    private void loadConstraintEnum(final DBSEntityReferrer refConstraint, Runnable onFinish) {
        loadJob = new KeyLoadJob("Load constraint '" + refConstraint.getName() + "' values", onFinish) {
            @Override
            List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException {
                final DBSEntityAttribute tableColumn = attribute.getEntityAttribute();
                if (tableColumn == null) {
                    return null;
                }
                final DBSEntityAttributeRef fkColumn = DBUtils.getConstraintAttribute(monitor, refConstraint, tableColumn);
                if (fkColumn == null) {
                    return null;
                }
                DBSEntityAssociation association;
                if (refConstraint instanceof DBSEntityAssociation) {
                    association = (DBSEntityAssociation) refConstraint;
                } else {
                    return null;
                }
                final DBSEntityAttribute refColumn = DBUtils.getReferenceAttribute(monitor, association, tableColumn, false);
                if (refColumn == null) {
                    return null;
                }
                final DBSEntityAttribute fkAttribute = fkColumn.getAttribute();
                final DBSEntityConstraint refConstraint = association.getReferencedConstraint();
                final DBSDictionary enumConstraint = (DBSDictionary) refConstraint.getParentObject();
                if (fkAttribute != null && enumConstraint != null) {
                    return enumConstraint.getDictionaryEnumeration(
                        monitor,
                        refColumn,
                        filterPattern,
                        null,
                        true,
                        true,
                        MAX_MULTI_VALUES);
                }
                return null;
            }

        };
        loadJob.schedule();
    }

    private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable, Runnable onFinish) {

        if (tableViewer.getTable().getColumns().length > 1)
            tableViewer.getTable().getColumn(1).setText("Count");
        loadJob = new KeyLoadJob("Load '" + attribute.getName() + "' values", onFinish) {

            private List<DBDLabelValuePair> result;

            @Override
            List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException {
                DBExecUtils.tryExecuteRecover(monitor, attributeEnumerable.getDataSource(), param -> {
                    try (DBCSession session = DBUtils.openUtilSession(monitor, attributeEnumerable, "Read value enumeration")) {
                        result = attributeEnumerable.getValueEnumeration(session, filterPattern, MAX_MULTI_VALUES, true);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                return result;
            }
        };
        loadJob.schedule();
    }

    private void loadMultiValueList(@NotNull Collection<DBDLabelValuePair> values, boolean mergeResultsWithData) {
        if (tableViewer == null || tableViewer.getControl() == null || tableViewer.getControl().isDisposed()) {
            return;
        }

        Pattern pattern = null;
        if (!CommonUtils.isEmpty(filterPattern) && attribute.getDataKind() == DBPDataKind.STRING) {
            pattern = Pattern.compile(SQLUtils.makeLikePattern("%" + filterPattern + "%"), Pattern.CASE_INSENSITIVE);
        }

        // Get all values from actual RSV data
        boolean hasNulls = false;
        Map<Object, DBDLabelValuePair> rowData = new HashMap<>();
        for (DBDLabelValuePair pair : values) {
            final DBDLabelValuePair oldLabel = rowData.get(pair.getValue());
            if (oldLabel != null) {
                // Duplicate label for single key - may happen in case of composite foreign keys
                String multiLabel = oldLabel.getLabel() + "," + pair.getLabel();
                if (multiLabel.length() > 200) {
                    multiLabel = multiLabel.substring(0, 200) + MULTI_KEY_LABEL;
                }
                rowData.put(pair.getValue(), new DBDLabelValuePair(multiLabel, pair.getValue()));
            } else {
                rowData.put(pair.getValue(), pair);
            }
        }
        if (mergeResultsWithData) {
            // Add values from fetched rows
            for (ResultSetRow row : viewer.getModel().getAllRows()) {
                Object cellValue = viewer.getModel().getCellValue(attribute, row);
                if (DBUtils.isNullValue(cellValue)) {
                    hasNulls = true;
                    continue;
                }
                if (!keyPresents(rowData, cellValue)) {
                    String itemString = attribute.getValueHandler().getValueDisplayString(attribute, cellValue, DBDDisplayFormat.UI);
                    rowData.put(cellValue, new DBDLabelValuePair(itemString, cellValue));
                }
            }
        }

        List<DBDLabelValuePair> sortedList = new ArrayList<>(rowData.values());
        if (pattern != null) {
            for (Iterator<DBDLabelValuePair> iter = sortedList.iterator(); iter.hasNext(); ) {
                final DBDLabelValuePair valuePair = iter.next();
                String itemString = attribute.getValueHandler().getValueDisplayString(attribute, valuePair.getValue(), DBDDisplayFormat.UI);
                if (!pattern.matcher(itemString).matches() && (valuePair.getLabel() == null || !pattern.matcher(valuePair.getLabel()).matches())) {
                    iter.remove();
                }
            }
        } else if (filterPattern != null && attribute.getDataKind() == DBPDataKind.NUMERIC) {
            // Filter numeric values
            double minValue = CommonUtils.toDouble(filterPattern);
            for (Iterator<DBDLabelValuePair> iter = sortedList.iterator(); iter.hasNext(); ) {
                final DBDLabelValuePair valuePair = iter.next();
                String itemString = attribute.getValueHandler().getValueDisplayString(attribute, valuePair.getValue(), DBDDisplayFormat.EDIT);
                double itemValue = CommonUtils.toDouble(itemString);
                if (itemValue < minValue) {
                    iter.remove();
                }
            }
        }
        try {
            Collections.sort(sortedList);
        } catch (Exception e) {
            // FIXME: This may happen in some crazy cases -
            // FIXME: error "Comparison method violates its general contract!" happens in case of long strings sorting
            // FIXME: Test on sakila.film.description
            log.error("Error sorting value collection", e);
        }
        if (hasNulls) {
            boolean nullPresents = false;
            for (DBDLabelValuePair val : rowData.values()) {
                if (DBUtils.isNullValue(val.getValue())) {
                    nullPresents = true;
                    break;
                }
            }
            if (!nullPresents) {
                sortedList.add(0, new DBDLabelValuePair(DBValueFormatting.getDefaultValueDisplayString(null, DBDDisplayFormat.UI), null));
            }
        }

        Set<Object> checkedValues = new HashSet<>();
        for (ResultSetRow row : rows) {
            Object value = viewer.getModel().getCellValue(attribute, row);
            checkedValues.add(value);
        }
        DBDAttributeConstraint constraint = viewer.getModel().getDataFilter().getConstraint(attribute);
        if (constraint != null && constraint.getOperator() == DBCLogicalOperator.IN) {
            //checkedValues.add(constraint.getValue());
            if (constraint.getValue() instanceof Object[]) {
                Collections.addAll(checkedValues, (Object[]) constraint.getValue());
            }
        }
        checkedValues.addAll(savedValues);

        tableViewer.setInput(sortedList);
        DBDLabelValuePair firstVisibleItem = null;

        if (isCheckedTable)
            for (DBDLabelValuePair row : sortedList) {
                Object cellValue = row.getValue();

                if (checkedValues.contains(cellValue)) {

                    TableItem t = (TableItem) tableViewer.testFindItem(row);

                    t.setChecked(true);
                    //((CheckboxTableViewer) tableViewer).setChecked(row, true);
                    if (firstVisibleItem == null) {
                        firstVisibleItem = row;
                    }
                }
            }

        ViewerColumnController vcc = ViewerColumnController.getFromControl(tableViewer.getTable());
        if (vcc != null) {
            vcc.repackColumns();
        } else {
            UIUtils.packColumns(tableViewer.getTable(), true);
        }
        if (firstVisibleItem != null) {
            final Widget item = tableViewer.testFindItem(firstVisibleItem);
            if (item != null) {
                tableViewer.getTable().setSelection((TableItem) item);
                tableViewer.getTable().showItem((TableItem) item);
            }
        }
    }

    private boolean keyPresents(Map<Object, DBDLabelValuePair> rowData, Object cellValue) {
        if (cellValue instanceof Number) {
            for (Object key : rowData.keySet()) {
                if (key instanceof Number && CommonUtils.compareNumbers((Number) key, (Number) cellValue) == 0) {
                    return true;
                }
            }
        }
        return rowData.containsKey(cellValue);
    }

    @Nullable
    public Object getFilterValue() {
        if (tableViewer != null) {
            Set<Object> values = new LinkedHashSet<>();

            for (DBDLabelValuePair item : getMultiValues()) {
                if (((TableItem)tableViewer.testFindItem(item)).getChecked()) {
                    values.add(item.getValue());
                }
            }
            values.addAll(savedValues);
            return values.toArray();
        } else if (editor != null) {
            try {
                return editor.extractEditorValue();
            } catch (DBException e) {
                log.error("Can't get editor value", e);
            }
        }
        return null;
    }

    public Button createFilterButton(String label, SelectionAdapter selectionAdapter) {
        if (isCheckedTable) {
            Button button = UIUtils.createDialogButton(buttonsPanel, label, selectionAdapter);
            ((GridLayout) buttonsPanel.getLayout()).numColumns++;
            return button;
        } else {
            return null;
        }
    }

    private abstract class KeyLoadJob extends AbstractJob {
        private final Runnable onFinish;
        KeyLoadJob(String name, Runnable onFinish) {
            super(name);
            this.onFinish = onFinish;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            final DBCExecutionContext executionContext = viewer.getExecutionContext();
            if (executionContext == null) {
                return Status.OK_STATUS;
            }
            try {
                final List<DBDLabelValuePair> valueEnumeration = readEnumeration(monitor);
                if (valueEnumeration == null) {
                    return Status.OK_STATUS;
                } else {
                    populateValues(valueEnumeration);
                }
                if (onFinish != null) {
                    onFinish.run();
                }
            } catch (Throwable e) {
                populateValues(Collections.emptyList());
                log.error(e);
            }
            return Status.OK_STATUS;
        }

        @Nullable
        abstract List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException;

        boolean mergeResultsWithData() {
            return CommonUtils.isEmpty(filterPattern);
        }

        void populateValues(@NotNull final Collection<DBDLabelValuePair> values) {
            UIUtils.asyncExec(() -> {
                loadMultiValueList(values, mergeResultsWithData());
            });
        }
    }
}
