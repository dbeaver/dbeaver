/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.panel;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.ui.gis.registry.LeafletTilesDescriptor;
import org.jkiss.dbeaver.utils.HelpUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

class TilesManagementDialog extends BaseDialog {
    private static final Log log = Log.getLog(TilesManagementDialog.class);

    private final List<LeafletTilesDescriptor> predefinedTiles;
    private final List<LeafletTilesDescriptor> userDefinedTiles;
    @Nullable
    private final LeafletTilesDescriptor oldSelectedTileLayer;
    @Nullable
    private LeafletTilesDescriptor currentSelectedTileLayer;
    private Tree tree;
    @Nullable
    private TreeItem predefinedTilesRootItem;
    @Nullable
    private TreeItem userDefinedTilesRootItem;
    private TreeItem lastSelectedTreeItem;
    private ToolItem viewOrEditTilesItem;
    private ToolItem deleteTilesItem;

    TilesManagementDialog(Shell parentShell) {
        super(parentShell, GISMessages.panel_select_tiles_action_manage_dialog_title, null);
        predefinedTiles = new ArrayList<>(GeometryViewerRegistry.getInstance().getPredefinedLeafletTiles());
        userDefinedTiles = new ArrayList<>(GeometryViewerRegistry.getInstance().getUserDefinedLeafletTiles());
        oldSelectedTileLayer = GeometryViewerRegistry.getInstance().getDefaultLeafletTiles();
        currentSelectedTileLayer = oldSelectedTileLayer;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.PROCEED_LABEL, true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        Composite composite = UIUtils.createComposite(dialogArea, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group group = UIUtils.createControlGroup(composite, "", 2, GridData.FILL_BOTH, 0);

        tree = new Tree(group, SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        UIUtils.createTreeColumn(tree, SWT.NONE, GISMessages.panel_select_tiles_action_manage_dialog_tiles_column_name);
        tree.setVisible(true);

        ToolBar toolBar = new ToolBar(group, SWT.VERTICAL);
        toolBar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        ToolItem addNewTilesItem = UIUtils.createToolItem(toolBar, GISMessages.panel_select_tiles_action_manage_dialog_toolbar_add_new_tiles, UIIcon.ADD, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TileLayerDefinitionDialog dialog = new TileLayerDefinitionDialog(getShell(), null);
                int status = dialog.open();
                if (status != IDialogConstants.OK_ID) {
                    return;
                }
                LeafletTilesDescriptor descriptor = dialog.getResultingTilesDescriptor();
                if (descriptor == null) {
                    log.error("New descriptor is null despite that user clicked ok");
                    return;
                }
                if (isModelContainsDescriptorWithLabel(descriptor.getLabel())) {
                    DBWorkbench.getPlatformUI().showError(
                            GISMessages.panel_select_tiles_action_manage_dialog_error_adding_new_tiles_title,
                            GISMessages.panel_select_tiles_action_manage_dialog_error_adding_new_tiles_message
                    );
                    return;
                }
                userDefinedTiles.add(descriptor);
                repopulateTree(descriptor, true);
            }
        });
        addNewTilesItem.setEnabled(true);

        viewOrEditTilesItem = UIUtils.createToolItem(toolBar, GISMessages.panel_select_tiles_action_manage_dialog_toolbar_view_or_edit_tiles, UIIcon.TEXTFIELD, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (isRootItem(lastSelectedTreeItem)) {
                    log.error("Can't find tiles to edit!");
                    return;
                }
                LeafletTilesDescriptor originalDescriptor = (LeafletTilesDescriptor) lastSelectedTreeItem.getData();
                TileLayerDefinitionDialog dialog = new TileLayerDefinitionDialog(getShell(), originalDescriptor);
                int result = dialog.open();
                if (result != IDialogConstants.OK_ID || originalDescriptor.isPredefined()) {
                    return;
                }
                LeafletTilesDescriptor editedDescriptor = dialog.getResultingTilesDescriptor();
                if (editedDescriptor == null) {
                    log.error("Edited descriptor is null despite that user clicked ok");
                    return;
                }
                if (containsDescriptorWithLabel(predefinedTiles, editedDescriptor.getLabel()) || userDefinedTiles.stream().anyMatch(t -> t.getLabel().equals(editedDescriptor.getLabel()) && !t.equals(originalDescriptor))) {
                    DBWorkbench.getPlatformUI().showError(
                            GISMessages.panel_select_tiles_action_manage_dialog_error_editing_tiles_title,
                            GISMessages.panel_select_tiles_action_manage_dialog_error_editing_tiles_message
                    );
                    return;
                }
                replace(userDefinedTiles, originalDescriptor, editedDescriptor);
                repopulateTree(editedDescriptor, true);
                if (originalDescriptor.equals(currentSelectedTileLayer)) {
                    currentSelectedTileLayer = editedDescriptor;
                }
            }
        });
        viewOrEditTilesItem.setEnabled(false);

        deleteTilesItem = UIUtils.createToolItem(toolBar, GISMessages.panel_select_tiles_action_manage_dialog_toolbar_delete_tiles, UIIcon.DELETE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (lastSelectedTreeItem == null || lastSelectedTreeItem.equals(predefinedTilesRootItem)) {
                    log.error("Can't find tiles to delete!");
                    return;
                }
                if (lastSelectedTreeItem.equals(userDefinedTilesRootItem)) {
                    userDefinedTiles.clear();
                    repopulateTree(null, true);
                    return;
                }
                LeafletTilesDescriptor descriptor = (LeafletTilesDescriptor) lastSelectedTreeItem.getData();
                if (descriptor.isPredefined()) {
                    log.error("Can't delete predefined descriptor!");
                    return;
                }
                int i = userDefinedTiles.indexOf(descriptor);
                if (i == -1) {
                    log.error("Can't delete predefined descriptor! It's not found in the model!");
                    return;
                }
                userDefinedTiles.remove(i);
                if (i < userDefinedTiles.size()) {
                    repopulateTree(userDefinedTiles.get(i), true);
                } else if (i == userDefinedTiles.size() && !userDefinedTiles.isEmpty()){
                    repopulateTree(userDefinedTiles.get(i - 1), true);
                } else {
                    repopulateTree(null, true);
                }
                if (descriptor.equals(currentSelectedTileLayer)) {
                    currentSelectedTileLayer = null;
                }
            }
        });
        deleteTilesItem.setEnabled(false);

        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!(e.item instanceof TreeItem)) {
                    return;
                }
                lastSelectedTreeItem = (TreeItem) e.item;
                if ((e.detail & SWT.CHECK) == SWT.CHECK) {
                    reactOnCheck(lastSelectedTreeItem);
                }
                changeToolbarState(lastSelectedTreeItem);
            }

            private void reactOnCheck(@NotNull TreeItem item) {
                if (isRootItem(item)) {
                    List<LeafletTilesDescriptor> list = item.equals(userDefinedTilesRootItem) ? userDefinedTiles : predefinedTiles;
                    LeafletTilesDescriptor lastSelectedDescriptor = null;
                    if (lastSelectedTreeItem != null && lastSelectedTreeItem.getData() instanceof LeafletTilesDescriptor) {
                        lastSelectedDescriptor = (LeafletTilesDescriptor) lastSelectedTreeItem.getData();
                    }
                    for (int i = 0; i < list.size(); i++) {
                        LeafletTilesDescriptor descriptor = list.get(i);
                        if (item.getChecked() != descriptor.isVisible()) {
                            LeafletTilesDescriptor newDescriptor = descriptor.withFlippedVisibility();
                            list.set(i, newDescriptor);
                            if (lastSelectedDescriptor != null && lastSelectedDescriptor.getId().equals(newDescriptor.getId())) {
                                lastSelectedDescriptor = newDescriptor;
                            }
                        }
                    }
                    repopulateTree(lastSelectedDescriptor, true);
                    return;
                }
                LeafletTilesDescriptor descriptor = ((LeafletTilesDescriptor) item.getData());
                LeafletTilesDescriptor withFlippedVisibility = descriptor.withFlippedVisibility();
                item.setData(withFlippedVisibility);
                List<LeafletTilesDescriptor> tilesContainer = descriptor.isPredefined() ? predefinedTiles : userDefinedTiles;
                replace(tilesContainer, descriptor, withFlippedVisibility);
                boolean checkOnRoot = tilesContainer.stream().anyMatch(LeafletTilesDescriptor::isVisible);
                if (descriptor.isPredefined()) {
                    if (predefinedTilesRootItem != null) {
                        predefinedTilesRootItem.setChecked(checkOnRoot);
                    } else {
                        log.error("Predefined item hangs without its root");
                    }
                } else if (userDefinedTilesRootItem != null) {
                    userDefinedTilesRootItem.setChecked(checkOnRoot);
                } else {
                    log.error("User defined item hangs without its root");
                }
            }
        });

        UIUtils.createInfoLabel(composite, GISMessages.panel_select_tiles_action_manage_dialog_infolabel_about_checkboxes_meaning);
        repopulateTree(null, false);

        return dialogArea;
    }

    private void changeToolbarState(@Nullable TreeItem item) {
        if (item == null) {
            viewOrEditTilesItem.setEnabled(false);
            deleteTilesItem.setEnabled(false);
            return;
        }
        if (item.getData() == null) {
            viewOrEditTilesItem.setEnabled(false);
            deleteTilesItem.setEnabled(item.equals(userDefinedTilesRootItem));
            return;
        }
        viewOrEditTilesItem.setEnabled(true);
        LeafletTilesDescriptor descriptor = (LeafletTilesDescriptor) item.getData();
        deleteTilesItem.setEnabled(!descriptor.isPredefined());
    }

    private static void replace(@NotNull List<LeafletTilesDescriptor> list, @NotNull LeafletTilesDescriptor what, @NotNull LeafletTilesDescriptor with) {
        for (int i = 0; i < list.size(); i++) {
            if (what.equals(list.get(i))) {
                list.set(i, with);
                return;
            }
        }
    }

    private boolean isRootItem(Widget widget) {
        return (predefinedTilesRootItem != null && predefinedTilesRootItem.equals(widget)) || (userDefinedTilesRootItem != null && userDefinedTilesRootItem.equals(widget));
    }

    private boolean isModelContainsDescriptorWithLabel(@NotNull String label) {
        return containsDescriptorWithLabel(predefinedTiles, label) || containsDescriptorWithLabel(userDefinedTiles, label);
    }

    private static boolean containsDescriptorWithLabel(@NotNull Collection<LeafletTilesDescriptor> collection, @NotNull String label) {
        return collection.stream().anyMatch(descriptor -> label.equals(descriptor.getLabel()));
    }

    private void repopulateTree(@Nullable LeafletTilesDescriptor tilesToSelect, boolean retainExpansion) {
        boolean expandPredefined = true;
        boolean expandUserDefined = true;
        if (retainExpansion) {
            TreeItem[] children = tree.getItems();
            for (TreeItem item: children) {
                if (GISMessages.panel_select_tiles_action_manage_dialog_predefined_tiles.equals(item.getText())) {
                    expandPredefined = item.getExpanded();
                    continue;
                }
                if (GISMessages.panel_select_tiles_action_manage_dialog_user_defined_tiles.equals(item.getText())) {
                    expandUserDefined = item.getExpanded();
                }
            }
        }
        tree.removeAll();
        predefinedTilesRootItem = null;
        userDefinedTilesRootItem = null;
        lastSelectedTreeItem = null;
        if (!predefinedTiles.isEmpty()) {
            predefinedTilesRootItem = new TreeItem(tree, SWT.NONE);
            predefinedTilesRootItem.setText(GISMessages.panel_select_tiles_action_manage_dialog_predefined_tiles);
            for (LeafletTilesDescriptor descriptor: predefinedTiles) {
                TreeItem item = new TreeItem(predefinedTilesRootItem, SWT.NONE);
                item.setData(descriptor);
                item.setText(descriptor.getLabel());
                item.setChecked(descriptor.isVisible());
                if (descriptor.equals(tilesToSelect)) {
                    lastSelectedTreeItem = item;
                }
            }
            predefinedTilesRootItem.setChecked(predefinedTiles.stream().anyMatch(LeafletTilesDescriptor::isVisible));
            predefinedTilesRootItem.setExpanded(expandPredefined);
        }
        if (!userDefinedTiles.isEmpty()) {
            userDefinedTilesRootItem = new TreeItem(tree, SWT.NONE);
            userDefinedTilesRootItem.setText(GISMessages.panel_select_tiles_action_manage_dialog_user_defined_tiles);
            for (LeafletTilesDescriptor descriptor: userDefinedTiles) {
                TreeItem item = new TreeItem(userDefinedTilesRootItem, SWT.NONE);
                item.setData(descriptor);
                item.setText(descriptor.getLabel());
                item.setChecked(descriptor.isVisible());
                if (descriptor.equals(tilesToSelect)) {
                    lastSelectedTreeItem = item;
                }
            }
            userDefinedTilesRootItem.setChecked(userDefinedTiles.stream().anyMatch(LeafletTilesDescriptor::isVisible));
            userDefinedTilesRootItem.setExpanded(expandUserDefined);
        }
        if (lastSelectedTreeItem == null) {
            lastSelectedTreeItem = predefinedTilesRootItem;
        }
        tree.setSelection(lastSelectedTreeItem);
        changeToolbarState(lastSelectedTreeItem);
        UIUtils.asyncExec(() -> UIUtils.packColumns(tree, true, new float[]{1.f}));
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            GeometryViewerRegistry.getInstance().updateTiles(predefinedTiles, userDefinedTiles);
            if (!Objects.equals(oldSelectedTileLayer, currentSelectedTileLayer)) {
                GeometryViewerRegistry.getInstance().setDefaultLeafletTiles(currentSelectedTileLayer);
            }
        }
        super.buttonPressed(buttonId);
    }

    private static class TileLayerDefinitionDialog extends BaseDialog {

        @Nullable
        private final LeafletTilesDescriptor originalTilesDescriptor;

        @Nullable
        private LeafletTilesDescriptor resultingTilesDescriptor;

        private Text labelText;
        private Text layersDefinitionText;

        TileLayerDefinitionDialog(Shell parentShell, @Nullable LeafletTilesDescriptor tilesDescriptor) {
            super(parentShell, getTitle(tilesDescriptor), null);
            this.originalTilesDescriptor = tilesDescriptor;
        }

        private static String getTitle(@Nullable LeafletTilesDescriptor tilesDescriptor) {
            if (tilesDescriptor == null) {
                return GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_add_tiles_title;
            }
            if (tilesDescriptor.isPredefined()) {
                return GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_view_tiles_title;
            }
            return GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_edit_tiles_title;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            Composite composite = UIUtils.createComposite(dialogArea, 1);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            Group group = UIUtils.createControlGroup(
                dialogArea,
                GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_tiles_properties_group,
                2,
                SWT.NONE,
                0
            );
            group.setLayoutData(new GridData(GridData.FILL_BOTH));

            int mutabilityStyle = originalTilesDescriptor != null && originalTilesDescriptor.isPredefined() ? SWT.READ_ONLY : SWT.NONE;
            labelText = UIUtils.createLabelText(
                group,
                GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_text_label_label,
                originalTilesDescriptor == null ? "" : originalTilesDescriptor.getLabel(),
                SWT.BORDER | mutabilityStyle
            );
            layersDefinitionText = UIUtils.createLabelText(
                group,
                GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_text_label_layers_definition,
                originalTilesDescriptor == null ? "" : originalTilesDescriptor.getLayersDefinition(),
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | mutabilityStyle
            );
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = UIUtils.getFontHeight(layersDefinitionText) * 15;
            gd.widthHint = UIUtils.getFontHeight(layersDefinitionText) * 60;
            layersDefinitionText.setLayoutData(gd);

            UIUtils.createLink(dialogArea, GISMessages.panel_select_tiles_action_manage_dialog_tile_layer_definition_dialog_layers_definition_explanation_link_text, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShellUtils.launchProgram(
                        HelpUtils.getHelpExternalReference("Working-with-Spatial-GIS-data#defining-custom-tile-layer"));
                }
            });

            return dialogArea;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

        @Nullable
        public LeafletTilesDescriptor getResultingTilesDescriptor() {
            return resultingTilesDescriptor;
        }

        @Override
        protected void buttonPressed(int buttonId) {
            if (buttonId == IDialogConstants.OK_ID) {
                resultingTilesDescriptor = LeafletTilesDescriptor.createUserDefined(
                    labelText.getText().trim(),
                    layersDefinitionText.getText().trim(),
                    originalTilesDescriptor == null || originalTilesDescriptor.isVisible()
                );
            }
            super.buttonPressed(buttonId);
        }
    }
}
