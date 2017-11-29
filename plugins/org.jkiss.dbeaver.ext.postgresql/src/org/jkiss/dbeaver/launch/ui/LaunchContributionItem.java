package org.jkiss.dbeaver.launch.ui;

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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.internal.launch.ui.DebugUiInternals;

public class LaunchContributionItem extends ContributionItem {

    private final String mode;
    
    // default launch group for this mode (null category)
    private ILaunchGroup defaultGroup = null;
    // map of launch groups by (non-null) categories, for this mode
    private Map<String, ILaunchGroup> groupsByCategory = null;

    protected LaunchContributionItem(String mode) {
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
    public void fill(Menu menu, int index) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            ISelection current = window.getSelectionService().getSelection();
            if (current instanceof IStructuredSelection) {
                IStructuredSelection structured = (IStructuredSelection) current;
                int accelerator = 1;
                List<ILaunchConfiguration> extracted = extractSharedConfigurations(structured.toArray());
                for (ILaunchConfiguration configuration : extracted) {
                    IAction action = DebugUiInternals.createConfigurationAction(configuration, mode, accelerator);
                    if (action != null) {
                        accelerator++;
                        ActionContributionItem item = new ActionContributionItem(action);
                        item.fill(menu, -1);
                    }
                }
                Map<IAction, String> shortcutActions = DebugUiInternals.createShortcutActions(structured, mode, accelerator);
            //we need a separator iff the shared config entry has been added and there are following shortcuts
                if(menu.getItemCount() > 0 && shortcutActions.size() > 0) {
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
                    ActionContributionItem item= new ActionContributionItem(action);
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
                            ActionContributionItem item= new ActionContributionItem(action);
                            item.fill(menu, -1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void fill(CoolBar parent, int index) {
        // TODO implement
        super.fill(parent, index);
    }

    @Override
    public void fill(ToolBar parent, int index) {
        // TODO implement
        super.fill(parent, index);
    }
    
    protected List<ILaunchConfiguration> extractSharedConfigurations(Object[] selection) {
        List<ILaunchConfiguration> configurations = new ArrayList<>();
        for (Object object : selection) {
            ILaunchConfiguration config = DebugUiInternals.isSharedConfig(object);
            if (config != null) {
                configurations.add(config);
            }
        }
        return configurations;
    }

}
