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
package org.jkiss.dbeaver.ui.controls.resultset.valuefilter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;


class GenericFilterValueEdit {

    private static final Log log = Log.getLog(GenericFilterValueEdit.class);

    TableViewer tableViewer;
    String filterPattern;

    private KeyLoadJob loadJob;
    IValueEditor editor;
    Text textControl;

    @NotNull
    final ResultSetViewer viewer;
    @NotNull
    final DBDAttributeBinding attr;
    @NotNull
    final ResultSetRow[] rows;
    @NotNull
    final DBCLogicalOperator operator;

    private boolean isCheckedTable;

    private static final int MAX_MULTI_VALUES = 1000;
    private static final String MULTI_KEY_LABEL = "...";


    GenericFilterValueEdit(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows, @NotNull DBCLogicalOperator operator) {
        this.viewer = viewer;
        this.attr = attr;
        this.rows = rows;
        this.operator = operator;
    }


    void setupTable(Composite composite, int style, boolean visibleLines, boolean visibleHeader, Object layoutData) {

        tableViewer = new TableViewer(composite, style);
        Table table = this.tableViewer.getTable();
        table.setLinesVisible(visibleLines);
        table.setHeaderVisible(visibleHeader);
        table.setLayoutData(layoutData);
        this.tableViewer.setContentProvider(new ListContentProvider());

        isCheckedTable = (style & SWT.CHECK) == SWT.CHECK;
    }

    void addContextMenu(Action[] actions) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            UIUtils.fillDefaultTableContextMenu(manager, tableViewer.getTable());
            manager.add(new Separator());

            for (Action act : actions) {
                manager.add(act);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        tableViewer.getTable().setMenu(menuMgr.createContextMenu(tableViewer.getTable()));
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
            loadValues();
        });
        return valueFilterText;
    }


    void loadValues() {
        if (loadJob != null) {
            loadJob.schedule(200);
            return;
        }
        // Load values
        final DBSEntityReferrer enumerableConstraint = ReferenceValueEditor.getEnumerableConstraint(attr);
        if (enumerableConstraint != null) {
            loadConstraintEnum(enumerableConstraint);
        } else if (attr.getEntityAttribute() instanceof DBSAttributeEnumerable) {
            loadAttributeEnum((DBSAttributeEnumerable) attr.getEntityAttribute());
        } else {
            loadMultiValueList(Collections.emptyList());
        }
    }

    private void loadConstraintEnum(final DBSEntityReferrer refConstraint) {
        loadJob = new KeyLoadJob("Load constraint '" + refConstraint.getName() + "' values") {
            @Override
            List<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException {
                final DBSEntityAttribute tableColumn = attr.getEntityAttribute();
                if (tableColumn == null) {
                    return null;
                }
                final DBSEntityAttributeRef fkColumn = DBUtils.getConstraintAttribute(session.getProgressMonitor(), refConstraint, tableColumn);
                if (fkColumn == null) {
                    return null;
                }
                DBSEntityAssociation association;
                if (refConstraint instanceof DBSEntityAssociation) {
                    association = (DBSEntityAssociation) refConstraint;
                } else {
                    return null;
                }
                final DBSEntityAttribute refColumn = DBUtils.getReferenceAttribute(session.getProgressMonitor(), association, tableColumn, false);
                if (refColumn == null) {
                    return null;
                }
                final DBSEntityAttribute fkAttribute = fkColumn.getAttribute();
                final DBSEntityConstraint refConstraint = association.getReferencedConstraint();
                final DBSConstraintEnumerable enumConstraint = (DBSConstraintEnumerable) refConstraint;
                if (fkAttribute != null && enumConstraint != null) {
                    return enumConstraint.getKeyEnumeration(
                        session,
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

    private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable) {

        if (tableViewer.getTable().getColumns().length > 1)
            tableViewer.getTable().getColumn(1).setText("Count");
        loadJob = new KeyLoadJob("Load '" + attr.getName() + "' values") {
            @Override
            List<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException {
                return attributeEnumerable.getValueEnumeration(session, filterPattern, MAX_MULTI_VALUES);
            }
        };
        loadJob.schedule();
    }

    private void loadMultiValueList(@NotNull Collection<DBDLabelValuePair> values) {
        if (tableViewer == null || tableViewer.getControl() == null || tableViewer.getControl().isDisposed()) {
            return;
        }

        Pattern pattern = null;
        if (!CommonUtils.isEmpty(filterPattern)) {
            pattern = Pattern.compile(SQLUtils.makeLikePattern("%" + filterPattern + "%"), Pattern.CASE_INSENSITIVE);
        }

        // Get all values from actual RSV data
        boolean hasNulls = false;
        java.util.Map<Object, DBDLabelValuePair> rowData = new HashMap<>();
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
        // Add values from fetched rows
        for (ResultSetRow row : viewer.getModel().getAllRows()) {
            Object cellValue = viewer.getModel().getCellValue(attr, row);
            if (DBUtils.isNullValue(cellValue)) {
                hasNulls = true;
                continue;
            }
            if (!rowData.containsKey(cellValue)) {
                String itemString = attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);
                rowData.put(cellValue, new DBDLabelValuePair(itemString, cellValue));
            }
        }

        java.util.List<DBDLabelValuePair> sortedList = new ArrayList<>(rowData.values());
        if (pattern != null) {
            for (Iterator<DBDLabelValuePair> iter = sortedList.iterator(); iter.hasNext(); ) {
                final DBDLabelValuePair valuePair = iter.next();
                String itemString = attr.getValueHandler().getValueDisplayString(attr, valuePair.getValue(), DBDDisplayFormat.UI);
                if (!pattern.matcher(itemString).matches() && (valuePair.getLabel() == null || !pattern.matcher(valuePair.getLabel()).matches())) {
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
            if (!rowData.containsKey(null)) {
                sortedList.add(0, new DBDLabelValuePair(DBValueFormatting.getDefaultValueDisplayString(null, DBDDisplayFormat.UI), null));
            }
        }

        Set<Object> checkedValues = new HashSet<>();
        for (ResultSetRow row : rows) {
            Object value = viewer.getModel().getCellValue(attr, row);
            checkedValues.add(value);
        }

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
        if (vcc != null)
            vcc.repackColumns();
        if (firstVisibleItem != null) {
            final Widget item = tableViewer.testFindItem(firstVisibleItem);
            if (item != null) {
                tableViewer.getTable().setSelection((TableItem) item);
                tableViewer.getTable().showItem((TableItem) item);
            }
        }
    }

    private abstract class KeyLoadJob extends AbstractJob {
        KeyLoadJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            final DBCExecutionContext executionContext = viewer.getExecutionContext();
            if (executionContext == null) {
                return Status.OK_STATUS;
            }
            try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Read value enumeration")) {
                final List<DBDLabelValuePair> valueEnumeration = readEnumeration(session);
                if (valueEnumeration == null) {
                    return Status.OK_STATUS;
                } else {
                    populateValues(valueEnumeration);
                }
            } catch (DBException e) {
                populateValues(Collections.emptyList());
                return GeneralUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }

        @Nullable
        abstract List<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException;

        void populateValues(@NotNull final Collection<DBDLabelValuePair> values) {
            UIUtils.asyncExec(() -> {
                loadMultiValueList(values);
            });
        }
    }
}
