package org.jkiss.dbeaver.ui.help;

import org.eclipse.help.IContext;
import org.eclipse.ui.help.AbstractHelpUI;

/**
 * Lightweight help UI
 */
public class LightweightHelpUI extends AbstractHelpUI {
    @Override
    public void displayHelp()
    {
    }

    @Override
    public void displayContext(IContext context, int x, int y)
    {
    }

    @Override
    public void displayHelpResource(String href)
    {
    }

    @Override
    public boolean isContextHelpDisplayed()
    {
        return false;
    }
}
