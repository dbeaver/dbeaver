/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.model;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIRequestFilter;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HttpHeadersField {
    private final List<String[]> entries = new ArrayList<>();
    private final TableViewer viewer;
    private final Label errorLabel;

    public HttpHeadersField(@NotNull Composite parent, @NotNull Runnable onChange) {
        Composite container = UIUtils.createComposite(parent, 1);
        GridData containerData = new GridData(SWT.FILL, SWT.TOP, true, false);
        containerData.horizontalSpan = 2;
        container.setLayoutData(containerData);
        UIUtils.createControlLabel(container, AIUIMessages.openai_configurator_custom_headers);

        Composite tableContainer = new Composite(container, SWT.NONE);
        TableColumnLayout columnLayout = new TableColumnLayout();
        tableContainer.setLayout(columnLayout);
        GridData tableData = new GridData(SWT.FILL, SWT.TOP, true, false);
        // keep column widths from increasing the preference page's preferred width
        tableData.widthHint = 0;
        tableData.heightHint = 100;
        tableContainer.setLayoutData(tableData);
        viewer = new TableViewer(tableContainer, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setInput(entries);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        errorLabel = new Label(container, SWT.WRAP);
        errorLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        GridData errorData = new GridData(SWT.FILL, SWT.TOP, true, false);
        errorData.widthHint = 0;
        errorData.exclude = true;
        errorLabel.setLayoutData(errorData);
        errorLabel.setVisible(false);
        createColumn(AIUIMessages.openai_configurator_header_name, 0, 35, columnLayout, onChange);
        createColumn(AIUIMessages.openai_configurator_header_value, 1, 65, columnLayout, onChange);

        ToolBar toolbar = new ToolBar(container, SWT.FLAT | SWT.HORIZONTAL);
        UIUtils.createToolItem(
            toolbar,
            AIUIMessages.openai_configurator_header_add,
            UIIcon.ROW_ADD,
            SelectionListener.widgetSelectedAdapter(event -> {
                String[] entry = {"", ""};
                entries.add(entry);
                viewer.refresh();
                viewer.editElement(entry, 0);
                headersChanged(onChange);
            })
        );
        ToolItem removeButton = UIUtils.createToolItem(
            toolbar,
            AIUIMessages.openai_configurator_header_remove,
            UIIcon.ROW_DELETE,
            SelectionListener.widgetSelectedAdapter(event -> {
                int index = table.getSelectionIndex();
                if (index >= 0) {
                    entries.remove(index);
                    viewer.refresh();
                    headersChanged(onChange);
                }
            })
        );
        removeButton.setEnabled(false);
        viewer.addSelectionChangedListener(event -> removeButton.setEnabled(!event.getSelection().isEmpty()));
    }

    private void createColumn(
        @NotNull String label,
        int index,
        int weight,
        @NotNull TableColumnLayout columnLayout,
        @NotNull Runnable onChange
    ) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(label);
        columnLayout.setColumnData(column.getColumn(), new ColumnWeightData(weight, 50));
        column.setLabelProvider(new ColumnLabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((String[]) element)[index];
            }
        });
        column.setEditingSupport(new EditingSupport(viewer) {
            @NotNull
            @Override
            protected CellEditor getCellEditor(@NotNull Object element) {
                return new TextCellEditor(viewer.getTable());
            }

            @Override
            protected boolean canEdit(@NotNull Object element) {
                return true;
            }

            @NotNull
            @Override
            protected Object getValue(@NotNull Object element) {
                return ((String[]) element)[index];
            }

            @Override
            protected void setValue(@NotNull Object element, @NotNull Object value) {
                ((String[]) element)[index] = CommonUtils.toString(value);
                viewer.update(element, null);
                headersChanged(onChange);
            }
        });
    }

    public void setHeaders(@NotNull Map<String, String> headers) {
        entries.clear();
        headers.forEach((name, value) -> entries.add(new String[] {name, value}));
        viewer.refresh();
        updateValidation();
    }

    public boolean isComplete() {
        return getErrorMessage() == null;
    }

    @Nullable
    public String getErrorMessage() {
        String invalidHeader = OpenAIRequestFilter.findInvalidHeader(getHeaders());
        return invalidHeader == null ? null : NLS.bind(AIUIMessages.openai_configurator_header_invalid, invalidHeader);
    }

    private void headersChanged(@NotNull Runnable onChange) {
        updateValidation();
        onChange.run();
    }

    private void updateValidation() {
        String error = getErrorMessage();
        errorLabel.setText(CommonUtils.notEmpty(error));
        errorLabel.setVisible(error != null);
        ((GridData) errorLabel.getLayoutData()).exclude = error == null;
        errorLabel.getParent().layout(true, true);
    }

    @NotNull
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String[] entry : entries) {
            String name = entry[0].trim();
            if (!name.isEmpty()) {
                headers.put(name, entry[1]);
            }
        }
        return headers;
    }
}
