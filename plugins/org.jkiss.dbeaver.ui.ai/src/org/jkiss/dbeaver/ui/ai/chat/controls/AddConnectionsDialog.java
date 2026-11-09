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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.controls.DatabaseLabelProviders;
import org.jkiss.dbeaver.ui.controls.NamedObjectPatternFilter;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class AddConnectionsDialog extends BaseDialog {
    private final List<? extends DBPDataSourceContainer> available;
    private final Set<DBPDataSourceContainer> selected = new LinkedHashSet<>();
    private final Set<DBPDataSourceContainer> existing;
    private CheckboxTableViewer viewer;

    AddConnectionsDialog(@NotNull Shell shell, @NotNull AIChatConversation conversation) {
        super(shell, AIChatMessagesUI.ai_chat_add_connections, null);
        DBPDataSourceContainer primary = conversation.getDataSource();
        existing = Set.copyOf(conversation.getDataSources());
        available = primary == null ? List.of() : primary.getProject().getDataSourceRegistry().getDataSources().stream()
            .filter(container -> !container.isTemporary())
            .sorted(Comparator.comparing(DBPDataSourceContainer::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);
        UIUtils.createLabel(area, AIChatMessagesUI.ai_chat_add_connections_description);
        Text filter = new Text(area, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
        filter.setMessage(AIChatMessagesUI.ai_chat_filter_connections);
        filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        viewer = CheckboxTableViewer.newCheckList(area, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 480;
        tableData.heightHint = 360;
        viewer.getTable().setLayoutData(tableData);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new DatabaseLabelProviders.ConnectionLabelProvider() {
            @Nullable
            @Override
            public Color getForeground(@NotNull Object element) {
                return existing.contains(element) ? getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DISABLED_FOREGROUND) : null;
            }
        });
        viewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isChecked(@NotNull Object element) {
                return existing.contains(element) || selected.contains(element);
            }

            @Override
            public boolean isGrayed(@NotNull Object element) {
                return existing.contains(element);
            }
        });
        NamedObjectPatternFilter patternFilter = new NamedObjectPatternFilter();
        viewer.addFilter(patternFilter);
        viewer.setInput(available);
        viewer.addCheckStateListener(event -> {
            DBPDataSourceContainer container = (DBPDataSourceContainer) event.getElement();
            if (existing.contains(container)) {
                viewer.setChecked(container, true);
                viewer.setGrayed(container, true);
                return;
            }
            if (event.getChecked()) {
                selected.add(container);
            } else {
                selected.remove(container);
            }
            updateSelection();
        });
        filter.addModifyListener(event -> {
            patternFilter.setPattern(filter.getText().strip());
            viewer.refresh();
        });
        filter.setFocus();
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.OK_ID, NLS.bind(AIChatMessagesUI.ai_chat_add_selected_count, available.size()), true);
        updateSelection();
    }

    private void updateSelection() {
        Button button = getButton(IDialogConstants.OK_ID);
        button.setEnabled(!selected.isEmpty());
        button.setText(selected.isEmpty() ? AIChatMessagesUI.ai_chat_add_selected
            : NLS.bind(AIChatMessagesUI.ai_chat_add_selected_count, selected.size()));
        button.requestLayout();
    }

    @NotNull
    List<DBPDataSourceContainer> getConnections() {
        return List.copyOf(selected);
    }
}
