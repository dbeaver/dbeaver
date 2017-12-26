/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.ui.actions.OpenLaunchDialogAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIInternals;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class LaunchContributionItem extends ContributionItem {

    private static final Object[] NO_OBJECTS = new Object[0];

    private final String mode;

    // default launch group for this mode (null category)
    private ILaunchGroup defaultGroup = null;
    // map of launch groups by (non-null) categories, for this mode
    private Map<String, ILaunchGroup> groupsByCategory = null;

    protected LaunchContributionItem(String mode)
    {
        this.mode = mode;
        ILaunchGroup[] groups = DebugUITools.getLaunchGroups();
        groupsByCategory = new HashMap<String, ILaunchGroup>(3);
        for (int i = 0; i < groups.length; i++) {
            ILaunchGroup group = groups[i];
            if (group.getMode().equals(mode)) {
                if (group.getCategory() == null) {
                    defaultGroup = group;
                } else {
                    groupsByCategory.put(group.getCategory(), group);
                }
            }
        }
    }

    @Override
    public void fill(Menu menu, int index)
    {
        Object[] selected = extractSelectedObjects();

        int accelerator = 1;
        List<ILaunchConfiguration> configurations = extractSharedConfigurations(selected);
        for (ILaunchConfiguration configuration : configurations) {
            IAction action = DebugUIInternals.createConfigurationAction(configuration, mode, accelerator);
            if (action != null) {
                accelerator++;
                ActionContributionItem item = new ActionContributionItem(action);
                item.fill(menu, -1);
            }
        }
        Map<IAction, String> shortcutActions = DebugUIInternals.createShortcutActions(selected, mode, accelerator);
        // we need a separator if the shared config entry has been added
        // and there are following shortcuts
        if (menu.getItemCount() > 0 && shortcutActions.size() > 0) {
            new MenuItem(menu, SWT.SEPARATOR);
        }
        List<String> categories = new ArrayList<String>();
        Set<IAction> actions = shortcutActions.keySet();
        for (IAction action : actions) {
            String category = shortcutActions.get(action);
            // NOTE: category can be null
            if (category != null && !categories.contains(category)) {
                categories.add(category);
            }
            ActionContributionItem item = new ActionContributionItem(action);
            item.fill(menu, -1);
        }

        // add in the open ... dialog shortcut(s)
        if (categories.isEmpty()) {
            if (defaultGroup != null) {
                if (accelerator > 1) {
                    new MenuItem(menu, SWT.SEPARATOR);
                }
                IAction action = new OpenLaunchDialogAction(defaultGroup.getIdentifier());
                ActionContributionItem item = new ActionContributionItem(action);
                item.fill(menu, -1);
            }
        } else {
            boolean addedSep = false;
            for (String category : categories) {
                ILaunchGroup group = defaultGroup;
                if (category != null) {
                    group = groupsByCategory.get(category);
                }
                if (group != null) {
                    if (accelerator > 1 && !addedSep) {
                        new MenuItem(menu, SWT.SEPARATOR);
                        addedSep = true;
                    }
                    IAction action = new OpenLaunchDialogAction(group.getIdentifier());
                    ActionContributionItem item = new ActionContributionItem(action);
                    item.fill(menu, -1);
                }
            }
        }
    }

    @Override
    public void fill(CoolBar parent, int index)
    {
        //AF: we are using standard contribution here for now
        super.fill(parent, index);
    }

    @Override
    public void fill(ToolBar parent, int index)
    {
        //AF: we are using standard contribution here for now
        super.fill(parent, index);
    }

    protected Object[] extractSelectedObjects()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return NO_OBJECTS;
        }

        ISelection selection = window.getSelectionService().getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            Object[] array = structured.toArray();
            Object o = structured.getFirstElement();
            if (o instanceof IEditorPart) {
                IEditorPart part = (IEditorPart) o;
                array[0] = part.getEditorInput();
            }
            return array;
        }
        if (selection instanceof ITextSelection) {
            IWorkbenchPage activePage = window.getActivePage();
            if (activePage != null) {
                IEditorPart activeEditor = activePage.getActiveEditor();
                DBSObject databaseObject = DebugUI.extractDatabaseObject(activeEditor);
                if (databaseObject != null) {
                    return new Object[] {databaseObject};
                }
            }
            
        }
        return NO_OBJECTS;
    }

    protected List<ILaunchConfiguration> extractSharedConfigurations(Object[] selection)
    {
        List<ILaunchConfiguration> configurations = new ArrayList<>();
        for (Object object : selection) {
            ILaunchConfiguration config = DebugUIInternals.isSharedConfig(object);
            if (config != null) {
                configurations.add(config);
            }
        }
        return configurations;
    }

}
