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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.DBSConstraintEnumerable;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;


class GenericFilterValueEdit {
    TableViewer table;
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

    public static final int MAX_MULTI_VALUES = 1000;
    public static final String MULTI_KEY_LABEL = "...";


    GenericFilterValueEdit(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows, @NotNull DBCLogicalOperator operator) {
        this.viewer = viewer;
        this.attr = attr;
        this.rows = rows;
        this.operator = operator;
    }


    void setupTable(Composite composite, int style, boolean visibleLines, boolean visibleHeader, Object layoutData) {

        table = new TableViewer(composite, style);
        table.getTable().setLinesVisible(visibleLines);
        table.getTable().setHeaderVisible(visibleHeader);
        table.getTable().setLayoutData(layoutData);
        table.setContentProvider(new ListContentProvider());

        isCheckedTable = (style & SWT.CHECK) == SWT.CHECK;
    }

    void addContextMenu(Action[] actions) {
        MenuManager menuMgr = new MenuManager();
        menuMgr.addMenuListener(manager -> {
            UIUtils.fillDefaultTableContextMenu(manager, table.getTable());
            manager.add(new Separator());

            for (Action act : actions) {
                manager.add(act);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        table.getTable().setMenu(menuMgr.createContextMenu(table.getTable()));
    }

    Collection<DBDLabelValuePair> getMultiValues() {
        return (Collection<DBDLabelValuePair>) table.getInput();
    }

    void addFilterTextbox(Composite composite) {

        // Create filter text
        final Text valueFilterText = new Text(composite, SWT.BORDER);
        valueFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        valueFilterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                filterPattern = valueFilterText.getText();
                if (filterPattern.isEmpty()) {
                    filterPattern = null;
                }
                loadValues();
            }
        });
    }


    void loadValues() {
        if (loadJob != null) {
            if (loadJob.getState() == Job.RUNNING) {
                loadJob.cancel();
            }
            loadJob.schedule(100);
            return;
        }
        // Load values
        final DBSEntityReferrer enumerableConstraint = ReferenceValueEditor.getEnumerableConstraint(attr);
        if (enumerableConstraint != null) {
            loadConstraintEnum(enumerableConstraint);
        } else if (attr.getEntityAttribute() instanceof DBSAttributeEnumerable) {
            loadAttributeEnum((DBSAttributeEnumerable) attr.getEntityAttribute());
        } else {
            loadMultiValueList(Collections.<DBDLabelValuePair>emptyList());
        }
    }

    private void loadConstraintEnum(final DBSEntityReferrer refConstraint) {
        loadJob = new KeyLoadJob("Load constraint '" + refConstraint.getName() + "' values") {
            @Override
            Collection<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException {
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
                        MAX_MULTI_VALUES);
                }
                return null;
            }
        };
        loadJob.schedule();
    }

    private void loadAttributeEnum(final DBSAttributeEnumerable attributeEnumerable) {

        if (table.getTable().getColumns().length > 1)
            table.getTable().getColumn(1).setText("Count");
        loadJob = new KeyLoadJob("Load '" + attr.getName() + "' values") {
            @Override
            Collection<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException {
                return attributeEnumerable.getValueEnumeration(session, filterPattern, MAX_MULTI_VALUES);
            }
        };
        loadJob.schedule();
    }

    private void loadMultiValueList(@NotNull Collection<DBDLabelValuePair> values) {
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
        Collections.sort(sortedList);
        if (pattern != null) {
            for (Iterator<DBDLabelValuePair> iter = sortedList.iterator(); iter.hasNext(); ) {
                final DBDLabelValuePair valuePair = iter.next();
                String itemString = attr.getValueHandler().getValueDisplayString(attr, valuePair.getValue(), DBDDisplayFormat.UI);
                if (!pattern.matcher(itemString).matches() && (valuePair.getLabel() == null || !pattern.matcher(valuePair.getLabel()).matches())) {
                    iter.remove();
                }
            }
        }
        Collections.sort(sortedList);
        if (hasNulls) {
            sortedList.add(0, new DBDLabelValuePair(DBValueFormatting.getDefaultValueDisplayString(null, DBDDisplayFormat.UI), null));
        }

        Set<Object> checkedValues = new HashSet<>();
        for (ResultSetRow row : rows) {
            Object value = viewer.getModel().getCellValue(attr, row);
            checkedValues.add(value);
        }

        table.setInput(sortedList);
        DBDLabelValuePair firstVisibleItem = null;

        if (isCheckedTable)
            for (DBDLabelValuePair row : sortedList) {
                Object cellValue = row.getValue();

                if (checkedValues.contains(cellValue)) {

                    TableItem t = (TableItem) table.testFindItem(row);

                    t.setChecked(true);
                    //((CheckboxTableViewer) table).setChecked(row, true);
                    if (firstVisibleItem == null) {
                        firstVisibleItem = row;
                    }
                }
            }

        ViewerColumnController vcc = ViewerColumnController.getFromControl(table.getTable());
        if (vcc != null)
            vcc.repackColumns();
        if (firstVisibleItem != null) {
            final Widget item = table.testFindItem(firstVisibleItem);
            if (item != null) {
                table.getTable().setSelection((TableItem) item);
                table.getTable().showItem((TableItem) item);
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
            try (DBCSession session = DBUtils.openUtilSession(monitor, executionContext.getDataSource(), "Read value enumeration")) {
                final Collection<DBDLabelValuePair> valueEnumeration = readEnumeration(session);
                if (valueEnumeration == null) {
                    return Status.OK_STATUS;
                } else {
                    populateValues(valueEnumeration);
                }
            } catch (DBException e) {
                populateValues(Collections.<DBDLabelValuePair>emptyList());
                return GeneralUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }

        @Nullable
        abstract Collection<DBDLabelValuePair> readEnumeration(DBCSession session) throws DBException;

        void populateValues(@NotNull final Collection<DBDLabelValuePair> values) {
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    loadMultiValueList(values);
                }
            });
        }
    }
}
