/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.CellEditor.LayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.IMenuService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLScriptCommitType;
import org.jkiss.dbeaver.model.sql.SQLScriptErrorHandling;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.ToolBarConfigurationDescriptor;
import org.jkiss.dbeaver.ui.actions.ToolBarConfigurationPropertyTester;
import org.jkiss.dbeaver.ui.actions.ToolBarConfigurationRegistry;
import org.jkiss.dbeaver.ui.controls.*;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.registry.ConfirmationDescriptor;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class PrefPageMenuCustomization extends AbstractPrefPage implements IWorkbenchPreferencePage {
    
    private abstract class CheckableNode extends TreeNode {

        private boolean isChecked;
        private boolean isGrayed;
        
        public CheckableNode(Object value) {
            super(value);
        }

        public boolean isChecked() {
            return isChecked;
        }
        
        public void setChecked(boolean value) {
            isChecked = value;
        }
        
        public boolean isGrayed() {
            return isGrayed;
        }
        
        public void setGrayed(boolean value) {
            isGrayed = value;
        }
    }
    
    private class ToolBarNode extends CheckableNode {
        ToolBarConfigurationDescriptor toolbar;
        List<ToolItemNode> subnodes;
        
        public ToolBarNode(ToolBarConfigurationDescriptor value) {
            super(value.getName());
            this.toolbar = value;
            this.subnodes = toolbar.items().stream().map(t -> new ToolItemNode(this, t)).collect(Collectors.toList());
            setChildren(subnodes.toArray(ToolItemNode[]::new));
            update();
        }
        
        public void update() {
            long checkedCount = subnodes.stream().filter(t -> t.isChecked()).count();
            if (checkedCount == 0) {
                super.setChecked(false);
                super.setGrayed(false);
            } else if (toolbar.items().size() == checkedCount) {
                super.setChecked(true);
                super.setGrayed(false);
            } else {
                super.setChecked(false);
                super.setGrayed(true);
            }
        }
        
        @Override
        public void setChecked(boolean value) {
            subnodes.forEach(n -> n.setChecked(value));
            super.setChecked(value);
        }
        
        @Override
        public String toString() {
            return toolbar.getName();
        }
    }
    
    private class ToolItemNode extends CheckableNode {
        final ToolBarConfigurationDescriptor.Item item;
        final String name;
        final ToolBarNode owner;
        
        public ToolItemNode(ToolBarNode owner, ToolBarConfigurationDescriptor.Item item) {
            super(item);
            this.item = item;
            this.name = getItemName(item);
            this.owner = owner;
            
            setChecked(item.isVisible());
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public void apply() {
            item.setVisible(isChecked());
        }
        
        public void restore() {
            setChecked(item.isVisible());
        }
        
        public void reset() {
            setChecked(item.isVisibleByDefault());
        }
    }
    
    
    private final ICommandService cmdSvc;
    private final Collection<String> knownCommands;
    private final List<ToolBarNode> toolBarNodes;

    private CheckboxTreeViewer treeViewer;
    
    public PrefPageMenuCustomization() {
        cmdSvc = PlatformUI.getWorkbench().getService(ICommandService.class);
        knownCommands = cmdSvc.getDefinedCommandIds();
        
        toolBarNodes = ToolBarConfigurationRegistry.getInstance().getKnownToolBars().stream()
            .map(t -> new ToolBarNode(t)).collect(Collectors.toList());
    }
    
    @Override
    public void init(IWorkbench workbench) {
    }

    private String getItemName(ToolBarConfigurationDescriptor.Item item) {
        if (item.getName() != null) {
            return item.getName();
        }
        if (item.getCommandId() != null && knownCommands.contains(item.getCommandId())) {
            try {
                return cmdSvc.getCommand(item.getCommandId()).getName();
            } catch (NotDefinedException e) {
                // TODO: log error
                e.printStackTrace();
            }
        }
        return item.getKey();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group groupEditors = UIUtils.createControlGroup(composite, "Toolbar items visiblity", 1, GridData.FILL_BOTH, 0);

        treeViewer = new CheckboxTreeViewer(groupEditors, SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.CHECK);
        treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        TreeViewerEditor.create(treeViewer, new ColumnViewerEditorActivationStrategy(treeViewer) { 
            @Override
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                return false;
            }
        }, 0);
        treeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                System.out.println(event);
            }
        });
        treeViewer.setCheckStateProvider(new ICheckStateProvider() {
            @Override
            public boolean isGrayed(Object element) { 
                if (element instanceof CheckableNode) {
                    return ((CheckableNode)element).isGrayed();
                }
                return false;
            }
            @Override
            public boolean isChecked(Object element) {
                if (element instanceof CheckableNode) {
                    return ((CheckableNode)element).isChecked();
                }
                return false;
            }
        });
        treeViewer.addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(CheckStateChangedEvent event) {
                Object node = event.getElement();   
                if (node instanceof CheckableNode) {
                    ((CheckableNode)node).setChecked(event.getChecked());
                    if (node instanceof ToolItemNode) {
                        ((ToolItemNode)node).owner.update();
                        treeViewer.refresh();
                    }
                    else if (node instanceof ToolBarNode) {
                        ((ToolBarNode)node).update();
                        treeViewer.refresh();
                    }
                }
            }
        });
        treeViewer.setContentProvider(new TreeNodeContentProvider());
        treeViewer.setInput(toolBarNodes.toArray(ToolBarNode[]::new));
        
        forEachToolItem(node -> node.restore());
        
        treeViewer.refresh();
        treeViewer.expandAll();
        
        return composite;
    }
    
    private void forEachToolItem(Consumer<ToolItemNode> action) {
        for (ToolBarNode toolBar: toolBarNodes) {
            for (ToolItemNode item: toolBar.subnodes) {
                action.accept(item);
            }   
            toolBar.update();
        }
        treeViewer.refresh();
    }
    
    @Override
    protected void performDefaults() {
        forEachToolItem(node -> node.reset());
    }

    @Override
    public final boolean performOk() {
        forEachToolItem(node -> node.apply());
        IPreferenceStore prefs = DBeaverActivator.getInstance().getPreferenceStore();
        if (prefs.needsSaving() && prefs instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore)prefs).save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ToolBarConfigurationPropertyTester.fireVisibilityPropertyChange();
        return true;
    }

}
