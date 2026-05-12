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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingAttribute;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingContainer;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingType;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ConfigureColumnsDialog extends BaseDialog {
    private final List<StreamMappingContainer> mappings;
    private final StreamConsumerSettings settings;

    private CheckboxTreeViewer viewer;
    private CLabel errorLabel;

    public ConfigureColumnsDialog(
        @NotNull Shell shell,
        @NotNull List<StreamMappingContainer> mappings,
        @NotNull StreamConsumerSettings settings
    ) {
        super(shell, DTUIMessages.stream_consumer_page_mapping_title, null);
        this.settings = settings;
        this.setShellStyle(SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL);
        this.mappings = mappings;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite group = super.createDialogArea(parent);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 400;
        gd.heightHint = 400;

        Composite composite = UIUtils.createComposite(group, 1);
        composite.setLayoutData(gd);

        viewer = new CheckboxTreeViewer(composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
        viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        viewer.getTree().setLinesVisible(false);
        viewer.getTree().setLayoutData(gd);


        viewer.setContentProvider(new TreeContentProvider() {
            @Override
            public Object[] getChildren(Object element) {
                // We have preloaded the attributes before, so it is 'safe' to use void monitor here
                return ((StreamMappingContainer) element).getAttributes(new VoidProgressMonitor()).toArray();
            }

            @Override
            public boolean hasChildren(Object element) {
                return element instanceof StreamMappingContainer;
            }
        });

        {
            TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
            column.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    final Object element = cell.getElement();
                    final DBPNamedObject object = (DBPNamedObject) element;
                    cell.setText(object.getName());
                    cell.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(object)));
                }
            });
            column.getColumn().setText(DTUIMessages.stream_consumer_page_mapping_name_column_name);
        }

        errorLabel = new CLabel(group, SWT.NONE);
        errorLabel.setText(DTUIMessages.stream_consumer_page_mapping_label_error_no_columns_selected_text);
        errorLabel.setImage(DBeaverIcons.getImage(DBIcon.SMALL_ERROR));
        errorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        UIUtils.asyncExec(() -> {
            viewer.setInput(mappings);
            viewer.expandAll(true);

            List<Object> checked = new ArrayList<>();
            for (StreamMappingContainer element : mappings) {
                final StreamMappingType type = element.getMappingType();
                if (type == StreamMappingType.export) {
                    checked.add(element);
                }
                for (StreamMappingAttribute attr : element.getAttributes(new VoidProgressMonitor())) {
                    if (attr.getMappingType() == StreamMappingType.export) {
                        checked.add(attr);
                    }
                }
            }
            viewer.setCheckedElements(checked.toArray());

            UIUtils.packColumns(viewer.getTree(), true, new float[] {0.75f, 0.25f});
            updateCompletion();
        });

        return group;
    }

    @Override
    protected void okPressed() {
        settings.getDataMappings().clear();

        Set<Object> checkedElements = Set.of(viewer.getCheckedElements());
        for (StreamMappingContainer container : mappings) {
            container.setMappingType(checkedElements.contains(container) ? StreamMappingType.export : StreamMappingType.skip);
            for (StreamMappingAttribute attr : container.getAttributes(new VoidProgressMonitor())) {
                attr.setMappingType(checkedElements.contains(attr) ? StreamMappingType.export : StreamMappingType.skip);
            }
        }

        for (StreamMappingContainer mapping : mappings) {
            settings.addDataMapping(mapping);
        }

        super.okPressed();
    }

    private void updateCompletion() {
        final boolean isComplete = mappings.stream().allMatch(StreamMappingContainer::isComplete);
        errorLabel.setVisible(!isComplete);
        enableButton(IDialogConstants.OK_ID, isComplete);
    }
}
