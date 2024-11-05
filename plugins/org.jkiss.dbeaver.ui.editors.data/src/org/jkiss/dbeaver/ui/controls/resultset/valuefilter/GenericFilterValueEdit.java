/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.UIWidgets;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;


class GenericFilterValueEdit {

    private static final Log log = Log.getLog(GenericFilterValueEdit.class);

    private TableViewer tableViewer;
    private String filterPattern;

    private IValueEditor editor;

    @NotNull
    private final ResultSetViewer viewer;
    @NotNull
    private final DBDAttributeBinding attribute;
    @NotNull
    private final ResultSetRow[] rows;
    @NotNull
    private final DBCLogicalOperator operator;

    private boolean isCheckedTable;

    private static final int INPUT_DELAY_BEFORE_LOAD = 300;
    private static final int MAX_MULTI_VALUES = 1000;
    private static final String MULTI_KEY_LABEL = "...";
    private Composite buttonsPanel;
    private Button toggleButton;

    private transient final Set<Object> savedValues = new HashSet<>();
    private boolean queryDatabase = true;
    private boolean showRowCount;
    private boolean showDistinctValuesCount;
    private boolean caseInsensitiveSearch;

    private transient volatile KeyLoadJob loadJob;

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
            updateToggleButton(toggleButton);
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
        UIWidgets.createTableContextMenu(tableViewer.getTable(), menu -> {
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

    Text addFilterText(Composite composite) {
        // Create job which will load values after specified delay
        final AbstractJob loadValuesJob = new AbstractJob("Load values timeout") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                UIUtils.asyncExec(() -> loadValues(null));
                return Status.OK_STATUS;
            }
        };
        loadValuesJob.setSystem(true);
        loadValuesJob.setUser(false);

        // Create filter text
        final Text valueFilterText = new Text(composite, SWT.BORDER);
        valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        valueFilterText.addModifyListener(e -> {
            filterPattern = valueFilterText.getText();
            if (filterPattern.isEmpty()) {
                filterPattern = null;
            }
            if (!loadValuesJob.isCanceled()) {
                loadValuesJob.cancel();
            }
            loadValuesJob.schedule(INPUT_DELAY_BEFORE_LOAD);
        });
        valueFilterText.addDisposeListener(e -> {
            KeyLoadJob curLoadJob = this.loadJob;
            if (curLoadJob != null) {
                if (!curLoadJob.isCanceled()) {
                    curLoadJob.cancel();
                }
            }
            if (!loadValuesJob.isCanceled()) {
                loadValuesJob.cancel();
            }
        });
        return valueFilterText;
    }

    void loadValues(@Nullable Consumer<Result> onFinish) {
        KeyLoadJob curLoadJob = this.loadJob;
        if (curLoadJob != null) {
            if (!curLoadJob.isCanceled()) {
                curLoadJob.cancel();
            }
            curLoadJob.schedule(200);
            return;
        }
        if (!queryDatabase) {
            loadMultiValueList(Collections.emptyList(), true, onFinish);
        } else {
            // Load values
            final DBSEntityReferrer enumerableConstraint = ResultSetUtils.getEnumerableConstraint(attribute);
            if (enumerableConstraint != null) {
                loadConstraintEnum(enumerableConstraint, onFinish);
            } else if (attribute.getEntityAttribute() instanceof DBSAttributeEnumerable) {
                loadAttributeEnum((DBSAttributeEnumerable) attribute.getEntityAttribute(), onFinish);
            } else if (attribute.getDataContainer() instanceof DBSDocumentAttributeEnumerable) {
                loadDictionaryEnum((DBSDocumentAttributeEnumerable) attribute.getDataContainer(), onFinish);
            } else {
                loadMultiValueList(Collections.emptyList(), true, onFinish);
            }
        }
    }

    private void loadConstraintEnum(final DBSEntityReferrer refConstraint, @Nullable Consumer<Result> onFinish) {
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
                final DBSDictionary enumConstraint = refConstraint == null ? null : (DBSDictionary) refConstraint.getParentObject();
                if (fkAttribute != null && enumConstraint != null) {
                    return enumConstraint.getDictionaryEnumeration(
                        monitor,
                        refColumn,
                        null,
                        filterPattern,
                        null,
                        true,
                        true,
                        caseInsensitiveSearch,
                        0,
                        MAX_MULTI_VALUES);
                }
                return null;
            }

        };
        loadJob.schedule();
    }

    private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable, @Nullable Consumer<Result> onFinish) {
        loadJob = new KeyLoadJob("Load '" + attribute.getName() + "' values", onFinish) {

            private List<DBDLabelValuePair> result;

            @Override
            List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException {
                DBExecUtils.tryExecuteRecover(monitor, attributeEnumerable.getDataSource(), param -> {
                    try (DBCSession session = DBUtils.openUtilSession(monitor, attributeEnumerable, "Read value enumeration")) {
                        result = attributeEnumerable.getValueEnumeration(
                            session,
                            filterPattern,
                            MAX_MULTI_VALUES,
                            showRowCount,
                            true,
                            caseInsensitiveSearch);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                return result;
            }

            @Nullable
            @Override
            protected Long readDistinctValuesCount(@NotNull DBRProgressMonitor monitor) throws DBException {
                final Long[] result = new Long[1];

                DBExecUtils.tryExecuteRecover(monitor, attributeEnumerable.getDataSource(), param -> {
                    try (DBCSession session = DBUtils.openUtilSession(monitor, attributeEnumerable, "Read count of distinct values")) {
                        result[0] = attributeEnumerable.getDistinctValuesCount(session);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });

                return result[0];
            }
        };
        loadJob.schedule();
    }

    private void loadDictionaryEnum(@NotNull DBSDocumentAttributeEnumerable dictionaryEnumerable, @Nullable Consumer<Result> onFinish) {
        loadJob = new KeyLoadJob("Load '" + attribute.getName() + "' values", onFinish) {
            @NotNull
            @Override
            List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException {
                final List<DBDLabelValuePair> result = new ArrayList<>();
                DBExecUtils.tryExecuteRecover(monitor, dictionaryEnumerable.getDataSource(), param -> {
                    try (DBCSession session = DBUtils.openUtilSession(monitor, dictionaryEnumerable, "Read value enumeration")) {
                        result.addAll(dictionaryEnumerable.getValueEnumeration(
                            session,
                            attribute,
                            filterPattern,
                            showRowCount,
                            caseInsensitiveSearch,
                            MAX_MULTI_VALUES
                        ));
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                return result;
            }
        };
        loadJob.schedule();
    }

    private void loadMultiValueList(@NotNull Collection<DBDLabelValuePair> values, boolean mergeResultsWithData, @Nullable Consumer<Result> onFinish) {
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
                DBDLabelValuePair dictValue = findValue(rowData, cellValue);
                if (dictValue == null && cellValue instanceof Date) {
                    // Date/time/timestamp types can have other string representation.
                    // And we can change it with the help of valueHandler
                    // We use here same format as date types have in values list
                    DBDValueHandler valueHandler = DBUtils.findValueHandler(attribute.getDataSource(), attribute);
                    String displayString = valueHandler.getValueDisplayString(attribute, cellValue, DBDDisplayFormat.UI);
                    dictValue = findValue(rowData, displayString);
                }
                if (dictValue == null) {
                    //String itemString = attribute.getValueHandler().getValueDisplayString(attribute, cellValue, DBDDisplayFormat.UI);
                    rowData.put(cellValue, new DBDLabelValuePairExt(null, cellValue, 1));
                } else if (values.isEmpty() && dictValue instanceof DBDLabelValuePairExt) {
                    // Inc local items count (only if we didn't read count from server, i.e. values are empty)
                    ((DBDLabelValuePairExt)dictValue).incCount();
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
            sortedList.sort(DBDLabelValuePair::compareTo);
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
        updateToggleButton(toggleButton);

        if (onFinish != null) {
            final Result result = new Result();
            if (showDistinctValuesCount) {
                result.setTotalDistinctCount((long) sortedList.size());
            }
            onFinish.accept(result);
        }
    }

    private DBDLabelValuePair findValue(Map<Object, DBDLabelValuePair> rowData, Object cellValue) {
        final DBDLabelValuePair value = rowData.get(cellValue);
        if (value != null) {
            // If we managed to found something at this point - return right away
            return value;
        }
        // Otherwise try to match values manually
        if (cellValue instanceof Number) {
            for (Map.Entry<Object, DBDLabelValuePair> pair : rowData.entrySet()) {
                if (pair.getKey() instanceof Number && CommonUtils.compareNumbers((Number) pair.getKey(), (Number) cellValue) == 0) {
                    return pair.getValue();
                }
            }
        }
        if (cellValue instanceof String) {
            for (Map.Entry<Object, DBDLabelValuePair> pair : rowData.entrySet()) {
                if (!DBUtils.isNullValue(pair.getKey()) && CommonUtils.toString(pair.getKey()).equals(cellValue)) {
                    return pair.getValue();
                }
            }
        }
        if (cellValue instanceof Timestamp) {
            for (Map.Entry<Object, DBDLabelValuePair> pair : rowData.entrySet()) {
                if (!DBUtils.isNullValue(pair.getKey())) {
                    Object key = pair.getKey();
                    try {
                        Timestamp timestamp = Timestamp.valueOf(key.toString());
                        if (timestamp.compareTo((Timestamp) cellValue) == 0) {
                            return pair.getValue();
                        }
                    } catch (Exception e) {
                        // Format exception maybe
                        // Continue
                    }
                }
            }
        }
        return rowData.get(cellValue.toString());
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

    @Nullable
    public Object getSelectedFilterValue() {
        if (tableViewer != null) {
            final Object selection = tableViewer.getStructuredSelection().getFirstElement();
            if (selection instanceof DBDLabelValuePair) {
                return new Object[]{((DBDLabelValuePair) selection).getValue()};
            }
        } else if (editor != null) {
            try {
                return editor.extractEditorValue();
            } catch (DBException e) {
                log.error("Can't get editor value", e);
            }
        }
        return null;
    }

    public Composite getButtonsPanel() {
        return buttonsPanel;
    }

    Button createFilterButton(String label, SelectionAdapter selectionAdapter) {
        if (isCheckedTable) {
            Button button = UIUtils.createDialogButton(buttonsPanel, label, selectionAdapter);
            ((GridLayout) buttonsPanel.getLayout()).numColumns++;
            return button;
        } else {
            return null;
        }
    }

    boolean isDictionarySelector() {
        return ResultSetUtils.getEnumerableConstraint(attribute) != null;
    }

    void setQueryDatabase(boolean queryDatabase) {
        this.queryDatabase = queryDatabase;
    }

    void setShowRowCount(boolean showRowCount) {
        this.showRowCount = showRowCount;
    }

    public void setShowDistinctValuesCount(boolean showDistinctValuesCount) {
        this.showDistinctValuesCount = showDistinctValuesCount;
    }

    public void setCaseInsensitiveSearch(boolean caseInsensitiveSearch) {
        this.caseInsensitiveSearch = caseInsensitiveSearch;
    }

    private abstract class KeyLoadJob extends AbstractJob {
        private final Consumer<Result> onFinish;
        KeyLoadJob(String name, @Nullable Consumer<Result> onFinish) {
            super(name);
            this.onFinish = onFinish;
            setSkipErrorOnCanceling(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            monitor.beginTask("Read filter values", 1);
            final DBCExecutionContext executionContext = viewer.getExecutionContext();
            if (executionContext == null) {
                return Status.OK_STATUS;
            }
            UIUtils.syncExec(() -> {
                final Table table = tableViewer.getTable();
                if (table != null && !table.isDisposed()) {
                    table.setEnabled(false);
                }
            });
            final Result result = new Result();
            if (showDistinctValuesCount) {
                try {
                    monitor.subTask("Read distinct values count");
                    result.setTotalDistinctCount(readDistinctValuesCount(monitor));
                } catch (Throwable e) {
                    log.error("Can't read count of distinct values", e);
                }
            }
            try {
                monitor.subTask("Read enumeration");
                List<DBDLabelValuePair> valueEnumeration = readEnumeration(monitor);
                if (valueEnumeration == null) {
                    populateValues(Collections.emptyList());
                    return Status.OK_STATUS;
                } else {
                    populateValues(valueEnumeration);
                }
                if (onFinish != null) {
                    onFinish.accept(result);
                }
            } catch (Throwable e) {
                populateValues(Collections.emptyList());
                log.error(e);
            } finally {
                monitor.done();
            }
            loadJob = null;
            return Status.OK_STATUS;
        }

        @Nullable
        abstract List<DBDLabelValuePair> readEnumeration(DBRProgressMonitor monitor) throws DBException;

        @Nullable
        protected Long readDistinctValuesCount(@NotNull DBRProgressMonitor monitor) throws DBException {
            return null;
        }

        boolean mergeResultsWithData() {
            return CommonUtils.isEmpty(filterPattern);
        }

        void populateValues(@NotNull final Collection<DBDLabelValuePair> values) {
            UIUtils.asyncExec(() -> {
                final Table table = tableViewer.getTable();
                if (table != null && !table.isDisposed()) {
                    loadMultiValueList(values, mergeResultsWithData(), null);
                    table.setEnabled(true);
                }
            });
        }
    }

    static class Result {
        private Long totalDistinctCount;

        public void setTotalDistinctCount(@Nullable Long totalDistinctCount) {
            this.totalDistinctCount = totalDistinctCount;
        }

        @Nullable
        public Long getTotalDistinctCount() {
            return totalDistinctCount;
        }
    }
}
