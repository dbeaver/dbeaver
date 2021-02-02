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
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.dbeaver.ui.gis.registry.LeafletTilesDescriptor;

import java.util.*;
import java.util.List;

class TilesManagementDialog extends BaseDialog {
    private static final Log log = Log.getLog(TilesManagementDialog.class);

    private final List<LeafletTilesDescriptor> predefinedTiles;
    private final List<LeafletTilesDescriptor> userDefinedTiles;
    ToolItem editTilesItem;
    ToolItem deleteTilesItem;
    private Tree tree;
    @Nullable
    private TreeItem predefinedTilesRootItem;
    @Nullable
    private TreeItem userDefinedTilesRootItem;
    @Nullable
    private TreeItem lastSelectedTreeItem;

    TilesManagementDialog(Shell parentShell) {
        super(parentShell, GISMessages.panel_select_tiles_action_manage_dialog_title, null);
        predefinedTiles = new ArrayList<>(GeometryViewerRegistry.getInstance().getPredefinedLeafletTiles());
        userDefinedTiles = new ArrayList<>(GeometryViewerRegistry.getInstance().getUserDefinedLeafletTiles());
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
                AddOrEditTileDialog dialog = new AddOrEditTileDialog(getShell(), null);
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
        editTilesItem = UIUtils.createToolItem(toolBar, GISMessages.panel_select_tiles_action_manage_dialog_toolbar_edit_tiles, UIIcon.EDIT, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (lastSelectedTreeItem == null || isRootItem(lastSelectedTreeItem)) {
                    log.error("Can't find tiles to edit!");
                    return;
                }
                LeafletTilesDescriptor descriptor = (LeafletTilesDescriptor) lastSelectedTreeItem.getData();
                if (descriptor.isPredefined()) {
                    log.error("Can't edit predefined descriptor!");
                    return;
                }
                AddOrEditTileDialog dialog = new AddOrEditTileDialog(getShell(), descriptor);
                int result = dialog.open();
                if (result != IDialogConstants.OK_ID) {
                    return;
                }
                LeafletTilesDescriptor editedDescriptor = dialog.getResultingTilesDescriptor();
                if (editedDescriptor == null) {
                    log.error("Edited descriptor is null despite that user clicked ok");
                    return;
                }
                if (isModelContainsDescriptorWithLabel(editedDescriptor.getLabel())) {
                    DBWorkbench.getPlatformUI().showError(
                            GISMessages.panel_select_tiles_action_manage_dialog_error_editing_tiles_title,
                            GISMessages.panel_select_tiles_action_manage_dialog_error_editing_tiles_message
                    );
                    return;
                }
                replace(userDefinedTiles, descriptor, editedDescriptor);
                repopulateTree(editedDescriptor, true);
            }
        });
        editTilesItem.setEnabled(false);
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
                } else {
                    repopulateTree(null, true);
                }
            }
        });
        deleteTilesItem.setEnabled(false);

        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!(e.item instanceof TreeItem)) {
                    lastSelectedTreeItem = null;
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
                    Arrays.stream(item.getItems()).forEach(treeItem -> treeItem.setChecked(item.getChecked()));
                    List<LeafletTilesDescriptor> list = item.equals(userDefinedTilesRootItem) ? userDefinedTiles : predefinedTiles;
                    for (int i = 0; i < list.size(); i++) {
                        LeafletTilesDescriptor descriptor = list.get(i);
                        if (item.getChecked() != descriptor.isVisible()) {
                            descriptor = descriptor.withFlippedVisibility();
                        }
                        list.set(i, descriptor);
                    }
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
            editTilesItem.setEnabled(false);
            deleteTilesItem.setEnabled(false);
            return;
        }
        if (item.getData() == null) {
            editTilesItem.setEnabled(false);
            deleteTilesItem.setEnabled(item.equals(userDefinedTilesRootItem));
            return;
        }
        LeafletTilesDescriptor descriptor = (LeafletTilesDescriptor) item.getData();
        editTilesItem.setEnabled(!descriptor.isPredefined());
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

    private boolean isRootItem(@NotNull Widget widget) {
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
        TreeItem toSelect = null;
        if (!predefinedTiles.isEmpty()) {
            predefinedTilesRootItem = new TreeItem(tree, SWT.NONE);
            predefinedTilesRootItem.setText(GISMessages.panel_select_tiles_action_manage_dialog_predefined_tiles);
            for (LeafletTilesDescriptor tile: predefinedTiles) {
                TreeItem item = new TreeItem(predefinedTilesRootItem, SWT.NONE);
                item.setData(tile);
                item.setText(tile.getLabel());
                item.setChecked(tile.isVisible());
                if (tile.equals(tilesToSelect)) {
                    toSelect = item;
                }
            }
            predefinedTilesRootItem.setChecked(predefinedTiles.stream().anyMatch(LeafletTilesDescriptor::isVisible));
            predefinedTilesRootItem.setExpanded(expandPredefined);
        }
        if (!userDefinedTiles.isEmpty()) {
            userDefinedTilesRootItem = new TreeItem(tree, SWT.NONE);
            userDefinedTilesRootItem.setText(GISMessages.panel_select_tiles_action_manage_dialog_user_defined_tiles);
            for (LeafletTilesDescriptor tile: userDefinedTiles) {
                TreeItem item = new TreeItem(userDefinedTilesRootItem, SWT.NONE);
                item.setData(tile);
                item.setText(tile.getLabel());
                item.setChecked(tile.isVisible());
                if (tile.equals(tilesToSelect)) {
                    toSelect = item;
                }
            }
            userDefinedTilesRootItem.setChecked(userDefinedTiles.stream().anyMatch(LeafletTilesDescriptor::isVisible));
            userDefinedTilesRootItem.setExpanded(expandUserDefined);
        }
        if (toSelect != null) {
            tree.setSelection(toSelect);
        }
        changeToolbarState(toSelect);
        UIUtils.asyncExec(() -> UIUtils.packColumns(tree, true, new float[]{1.f}));
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            GeometryViewerRegistry.getInstance().updateTiles(predefinedTiles, userDefinedTiles);
        }
        super.buttonPressed(buttonId);
    }

    private static class AddOrEditTileDialog extends BaseDialog {
        @Nullable
        private final LeafletTilesDescriptor originalTilesDescriptor;

        @Nullable
        private LeafletTilesDescriptor resultingTilesDescriptor;

        Text labelText;
        Text layersDefinitionText;

        AddOrEditTileDialog(Shell parentShell, @Nullable LeafletTilesDescriptor tilesDescriptor) {
            super(parentShell, getTitle(tilesDescriptor), null);
            this.originalTilesDescriptor = tilesDescriptor;
        }

        private static String getTitle(@Nullable LeafletTilesDescriptor tilesDescriptor) {
            if (tilesDescriptor == null) {
                return GISMessages.panel_select_tiles_action_manage_dialog_add_or_edit_tiles_dialog_add_tiles_title;
            }
            return GISMessages.panel_select_tiles_action_manage_dialog_add_or_edit_tiles_dialog_edit_tiles_title;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);

            Group group = UIUtils.createControlGroup(
                dialogArea,
                GISMessages.panel_select_tiles_action_manage_dialog_add_or_edit_tiles_dialog_tiles_properties_group,
                2,
                SWT.NONE,
                0
            );
            group.setLayoutData(new GridData(GridData.FILL_BOTH));

            String label = "";
            String layersDefinition = "";
            if (originalTilesDescriptor != null) {
                label = originalTilesDescriptor.getLabel();
                layersDefinition = originalTilesDescriptor.getLayersDefinition();
            }
            labelText = UIUtils.createLabelText(
                group,
                GISMessages.panel_select_tiles_action_manage_dialog_add_or_edit_tiles_dialog_text_label_label,
                label,
                SWT.BORDER
            );
            layersDefinitionText = UIUtils.createLabelText(
                group,
                GISMessages.panel_select_tiles_action_manage_dialog_add_or_edit_tiles_dialog_text_label_layers_definition,
                layersDefinition,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP
            );
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = UIUtils.getFontHeight(layersDefinitionText) * 15;
            gd.widthHint = UIUtils.getFontHeight(layersDefinitionText) * 40;
            layersDefinitionText.setLayoutData(gd);

            //todo info label with link to wiki page
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
                    labelText.getText(),
                    layersDefinitionText.getText(),
                    originalTilesDescriptor == null || originalTilesDescriptor.isVisible()
                );
            }
            super.buttonPressed(buttonId);
        }
    }
}
