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
package org.jkiss.dbeaver.ui.tracking;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sync.DBPSyncScope;
import org.jkiss.dbeaver.model.tracking.sync.DDPartSelection;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.tracking.internal.DDTrackingUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class DDCreateConfigurationDialog extends BaseDialog {

    private final List<DDPartSelection> availableParts;
    private final List<Object> rootElements = new ArrayList<>();

    private CheckboxTreeViewer treeViewer;
    private Text nameText;
    private String name;
    private List<String> selectedKeys = List.of();

    public DDCreateConfigurationDialog(@NotNull Shell parentShell, @NotNull List<DDPartSelection> availableParts) {
        super(parentShell, DDTrackingUIMessages.create_configuration_dialog_title, null);
        this.availableParts = availableParts;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite composite = super.createDialogArea(parent);

        UIUtils.createControlLabel(composite, DDTrackingUIMessages.create_configuration_dialog_name_label);
        nameText = new Text(composite, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        UIUtils.createControlLabel(composite, DDTrackingUIMessages.create_configuration_dialog_include_label);

        List<DDPartSelection> projectParts = new ArrayList<>();
        for (DDPartSelection part : availableParts) {
            if (part.scope() == DBPSyncScope.PROJECT) {
                projectParts.add(part);
            } else {
                rootElements.add(part);
            }
        }
        if (!projectParts.isEmpty()) {
            rootElements.add(new ProjectGroup(projectParts));
        }

        treeViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
        treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        treeViewer.setContentProvider(new PartTreeContentProvider());
        treeViewer.setLabelProvider(new PartTreeLabelProvider());
        treeViewer.setInput(rootElements);
        treeViewer.expandAll();
        treeViewer.setCheckedElements(allElements().toArray());
        treeViewer.addCheckStateListener(event -> {
            if (event.getElement() instanceof ProjectGroup group) {
                treeViewer.setSubtreeChecked(group, event.getChecked());
                treeViewer.setGrayed(group, false);
            } else {
                updateProjectGroupCheckState();
            }
        });

        return composite;
    }

    @NotNull
    private List<Object> allElements() {
        List<Object> elements = new ArrayList<>(rootElements);
        for (Object element : rootElements) {
            if (element instanceof ProjectGroup group) {
                elements.addAll(group.parts());
            }
        }
        return elements;
    }

    private void updateProjectGroupCheckState() {
        for (Object element : rootElements) {
            if (element instanceof ProjectGroup group) {
                boolean anyChecked = false;
                boolean allChecked = true;
                for (DDPartSelection part : group.parts()) {
                    if (treeViewer.getChecked(part)) {
                        anyChecked = true;
                    } else {
                        allChecked = false;
                    }
                }
                treeViewer.setChecked(group, anyChecked);
                treeViewer.setGrayed(group, anyChecked && !allChecked);
            }
        }
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, DDTrackingUIMessages.create_configuration_dialog_create_button, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed() {
        String value = nameText.getText().trim();
        if (CommonUtils.isEmpty(value)) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (Object element : treeViewer.getCheckedElements()) {
            if (element instanceof DDPartSelection part) {
                keys.add(part.key());
            }
        }
        if (keys.isEmpty()) {
            return;
        }
        name = value;
        selectedKeys = keys;
        super.okPressed();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public List<String> getSelectedKeys() {
        return selectedKeys;
    }

    private record ProjectGroup(@NotNull List<DDPartSelection> parts) {
    }

    private static class PartTreeContentProvider implements ITreeContentProvider {
        @NotNull
        @Override
        public Object[] getElements(@NotNull Object inputElement) {
            return ((List<?>) inputElement).toArray();
        }

        @NotNull
        @Override
        public Object[] getChildren(@NotNull Object parentElement) {
            return parentElement instanceof ProjectGroup group ? group.parts().toArray() : new Object[0];
        }

        @Nullable
        @Override
        public Object getParent(@NotNull Object element) {
            return null;
        }

        @Override
        public boolean hasChildren(@NotNull Object element) {
            return element instanceof ProjectGroup;
        }
    }

    private static class PartTreeLabelProvider extends LabelProvider {
        @NotNull
        @Override
        public String getText(@Nullable Object element) {
            if (element instanceof DDPartSelection part) {
                return part.displayName();
            }
            if (element instanceof ProjectGroup) {
                return DDTrackingUIMessages.create_configuration_dialog_project_group;
            }
            return String.valueOf(element);
        }
    }
}
