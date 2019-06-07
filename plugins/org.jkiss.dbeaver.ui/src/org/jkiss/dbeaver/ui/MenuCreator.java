package org.jkiss.dbeaver.ui;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.jkiss.dbeaver.model.runtime.DBRCreator;

public class MenuCreator implements IMenuCreator {

    private DBRCreator<MenuManager, Control> creator;
    private MenuManager menuManager;

    public MenuCreator(DBRCreator<MenuManager, Control> creator) {
        this.creator = creator;
    }

    @Override
    public Menu getMenu(Control parent) {
        if (menuManager != null) {
            menuManager.dispose();
        }
        menuManager = creator.createObject(parent);
        return menuManager.createContextMenu(parent);
    }

    @Override
    public Menu getMenu(Menu parent) {
        return null;
    }

    @Override
    public void dispose() {
        if (menuManager != null) {
            menuManager.dispose();
        }
        menuManager = null;
    }

}
