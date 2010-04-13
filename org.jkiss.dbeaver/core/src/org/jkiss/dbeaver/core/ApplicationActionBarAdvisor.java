package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.action.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.jkiss.dbeaver.ui.actions.ToggleViewAction;
import org.jkiss.dbeaver.ui.actions.sql.ExecuteScriptAction;
import org.jkiss.dbeaver.ui.actions.sql.ExecuteStatementAction;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the
 * actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor
{

    // Actions - important to allocate these only in makeActions, and then use them
    // in the fill methods.  This ensures that the actions aren't recreated
    // when fillActionBars is called with FILL_PROXY.
    private IWorkbenchAction exitAction;
    private IWorkbenchAction aboutAction;
    private IWorkbenchAction newWindowAction;
    //private IWorkbenchAction viewPropertiesAction;
    private IWorkbenchAction viewPreferencesAction;
    //private DriverManagerAction driverManagerAction;
    //private NewConnectionAction newConnectionAction;
    //private OpenSQLEditorAction openSQLEditorAction;

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer)
    {
        super(configurer);
    }

    @SuppressWarnings("restriction")
	private void removeActionExtension(String actionSetId)
    {
        ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
        IActionSetDescriptor[] actionSets = reg.getActionSets();
        for (IActionSetDescriptor actionSet : actionSets) {
            if (!actionSet.getId().equals(actionSetId)) {
                continue;
            }
            IExtension ext = actionSet.getConfigurationElement().getDeclaringExtension();
            reg.removeExtension(ext, new Object[]{actionSet});
        }
    }

    protected void makeActions(final IWorkbenchWindow window)
    {
        // Remove annotations actions
        removeActionExtension("org.eclipse.ui.edit.text.actionSet.annotationNavigation"); //$NON-NLS-1$
        // Removing annoying gotoLastPosition Message.
        removeActionExtension("org.eclipse.ui.edit.text.actionSet.navigation"); //$NON-NLS-1$
        // Removing convert line delimiters menu.
        removeActionExtension("org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo"); //$NON-NLS-1$
        // Removing convert line delimiters menu.
        removeActionExtension("org.eclipse.ui.actionSet.openFiles"); //$NON-NLS-1$

        // Creates the actions and registers them.
        // Registering is needed to ensure that key bindings work.
        // The corresponding commands keybindings are defined in the plugin.xml file.
        // Registering also provides automatic disposal of the actions when
        // the window is closed.

        exitAction = ActionFactory.QUIT.create(window);
        register(exitAction);

        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);

        newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        register(newWindowAction);

        //viewPropertiesAction = ActionFactory.PROPERTIES.create(window);
        //register(viewPropertiesAction);

        viewPreferencesAction = ActionFactory.PREFERENCES.create(window);
        register(viewPreferencesAction);

        //newConnectionAction = new NewConnectionAction(window);
        //register(newConnectionAction);

        //driverManagerAction = new DriverManagerAction(window);
        //register(driverManagerAction);

        //openSQLEditorAction = new OpenSQLEditorAction(window);
        //register(openSQLEditorAction);

        register(new ExecuteStatementAction());
        register(new ExecuteScriptAction());
    }

    protected void fillMenuBar(IMenuManager menuBar)
    {
        MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
        MenuManager viewMenu = new MenuManager("&View", "viewMenu");
        MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
        MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        // Add a group marker indicating where action set menus will appear.
        menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        // File
        fileMenu.add(newWindowAction);
        //fileMenu.add(new Separator());
        //fileMenu.add(driverManagerAction);
        //fileMenu.add(newConnectionAction);
        fileMenu.add(new Separator());
        fileMenu.add(viewPreferencesAction);
        fileMenu.add(new Separator());
        fileMenu.add(exitAction);

        // Edit
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        editMenu.add(new Separator(IWorkbenchActionConstants.FIND_EXT));

        //editMenu.add(ActionFactory.PROPERTIES);
        //editMenu.add(viewPropertiesAction);

        // View
        viewMenu.add(new ToggleViewAction("Error Log", "org.eclipse.pde.runtime.LogView"));
        viewMenu.add(new ToggleViewAction("Console", "org.jkiss.dbeaver.core.consoleView"));
        viewMenu.add(new ToggleViewAction("Properties", "org.jkiss.dbeaver.core.propertiesView"));
        viewMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        viewMenu.add(new Separator());

        // Help
        helpMenu.add(aboutAction);
    }

    protected void fillCoolBar(ICoolBarManager coolBar)
    {
        //IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        //coolBar.add(new ToolBarContributionItem(toolbar, "main"));
        //toolbar.add(driverManagerAction);
        //toolbar.add(newConnectionAction);
        //toolbar.add(openSQLEditorAction);
    }
}
