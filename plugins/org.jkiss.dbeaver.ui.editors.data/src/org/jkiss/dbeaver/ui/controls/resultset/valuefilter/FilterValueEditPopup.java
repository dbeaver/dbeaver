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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;

public class FilterValueEditPopup extends Dialog {

    private static final String DIALOG_ID = "DBeaver.FilterValueEditMenu";//$NON-NLS-1$

    private Object value;
    private GenericFilterValueEdit filter;
    private Point location;

    public FilterValueEditPopup(Shell parentShell, @NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow[] rows) {
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
        getShell().setText("Filter by '" + filter.attr.getFullyQualifiedName(DBPEvaluationContext.UI) + "'");

        DBSEntityReferrer descReferrer = ReferenceValueEditor.getEnumerableConstraint(filter.attr);

        Composite group = (Composite) super.createDialogArea(parent);
        {
            Composite labelComposite = UIUtils.createComposite(group, 2);
            labelComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Label controlLabel = UIUtils.createControlLabel(labelComposite, "Choose value(s) to filter by");
            controlLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (descReferrer instanceof DBSEntityAssociation) {
                Link hintLabel = UIUtils.createLink(labelComposite, "(<a>Define Description</a>)", new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        EditDictionaryPage editDictionaryPage = new EditDictionaryPage(((DBSEntityAssociation) descReferrer).getAssociatedEntity());
                        if (editDictionaryPage.edit(parent.getShell())) {
                            filter.loadValues();
                        }
                    }
                });
                hintLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            } else {
                UIUtils.createEmptyLabel(labelComposite, 1, 1);
            }
        }

        Composite tableComposite = new Composite(group, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        tableComposite.setLayoutData(gd);
        tableComposite.setLayout(new FillLayout());

        filter.setupTable(tableComposite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL, true, descReferrer != null, new GridData(GridData.FILL_BOTH));
        Table table = filter.tableViewer.getTable();

        TableViewerColumn resultsetColumn = new TableViewerColumn(filter.tableViewer, UIUtils.createTableColumn(table, SWT.NONE, "Value"));
        resultsetColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return filter.attr.getValueHandler().getValueDisplayString(filter.attr, ((DBDLabelValuePair) element).getValue(), DBDDisplayFormat.UI);
            }
        });

        TableViewerColumn descColumn = null;
        if (descReferrer != null) {
            descColumn = new TableViewerColumn(filter.tableViewer, UIUtils.createTableColumn(table, SWT.NONE, "Description"));
            descColumn.setLabelProvider(new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBDLabelValuePair) element).getLabel();
                }
            });
        }

        // Resize the column to fit the contents
        UIUtils.asyncExec(() -> {
            UIUtils.packColumns(table, true);
        });

        FocusAdapter focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                UIUtils.asyncExec(() -> {
                    if (!UIUtils.isParent(getShell(), getShell().getDisplay().getFocusControl())) {
                        cancelPressed();
                    }
                });
            }
        };
        table.addFocusListener(focusListener);

        filter.tableViewer.addSelectionChangedListener(event -> {
            ISelection selection = event.getSelection();
            if (selection instanceof IStructuredSelection) {
                value = ((IStructuredSelection) selection).getFirstElement();
            }
            //okPressed();
        });
        filter.tableViewer.addDoubleClickListener(event -> {
            value = filter.tableViewer.getStructuredSelection().getFirstElement();
            okPressed();
        });

        if (filter.attr.getDataKind() == DBPDataKind.STRING || descReferrer != null) {
            Text filterTextbox = filter.addFilterTextbox(group);
            filterTextbox.setFocus();
            filterTextbox.addTraverseListener(e -> {
                if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS || e.detail == SWT.TRAVERSE_ARROW_NEXT) {
                    if (table.getSelectionIndex() < 0 && table.getItemCount() > 0) {
                        table.setSelection(0);
                    }
                    table.setFocus();
                }
            });
            filterTextbox.addFocusListener(focusListener);
        } else {
            table.setFocus();
        }
        filter.filterPattern = null;
        filter.loadValues();


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
