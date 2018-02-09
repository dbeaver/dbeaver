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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class FilterValueEditMenu extends Dialog {

    private static final String DIALOG_ID = "DBeaver.FilterValueEditMenu";//$NON-NLS-1$

    private Object value;
    private GenericFilterValueEdit filter;
    private Point location;

    public FilterValueEditMenu(Shell parentShell, @NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows) {
        super(parentShell);
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
        getShell().setText("Filter by column value");

        Composite group = (Composite) super.createDialogArea(parent);
        UIUtils.createControlLabel(group, "Choose value(s) to filter by");


        Composite tableComposite = new Composite(group, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        tableComposite.setLayoutData(gd);
        tableComposite.setLayout(new GridLayout(1, false));

        filter.setupTable(tableComposite, SWT.BORDER | SWT.SINGLE | SWT.NO_SCROLL | SWT.V_SCROLL, true, false, new GridData(GridData.FILL_BOTH));
        Table table = filter.table.getTable();

        TableViewerColumn resultsetColumn = new TableViewerColumn(filter.table, SWT.NONE);
        resultsetColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return filter.attr.getValueHandler().getValueDisplayString(filter.attr, ((DBDLabelValuePair) element).getValue(), DBDDisplayFormat.UI);
            }
        });

        resultsetColumn.getColumn().setResizable(false);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComposite.setLayout(tableLayout);

        // Resize the column to fit the contents
        resultsetColumn.getColumn().pack();
        int resultsetWidth = resultsetColumn.getColumn().getWidth();
        // Set  column to fill 100%, but with its packed width as minimum
        tableLayout.setColumnData(resultsetColumn.getColumn(), new ColumnWeightData(100, resultsetWidth));

        FocusAdapter focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                DBeaverUI.asyncExec(() -> {
                    if (!UIUtils.isParent(getShell(), getShell().getDisplay().getFocusControl())) {
                        cancelPressed();
                    }
                });
            }
        };
        table.addFocusListener(focusListener);

        filter.table.addSelectionChangedListener(event -> {
            value = event.getStructuredSelection().getFirstElement();
            okPressed();
        });
        filter.table.addDoubleClickListener(event -> {
            value = filter.table.getStructuredSelection().getFirstElement();
            okPressed();
        });


        if (filter.attr.getDataKind() == DBPDataKind.STRING) {
            filter.addFilterTextbox(parent);
        }
        filter.filterPattern = null;
        filter.loadValues();
        table.setFocus();

        return tableComposite;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        return UIUtils.createPlaceholder(parent, 1);
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public Object getValue() {
        return ((DBDLabelValuePair) value).getValue();
    }
}
