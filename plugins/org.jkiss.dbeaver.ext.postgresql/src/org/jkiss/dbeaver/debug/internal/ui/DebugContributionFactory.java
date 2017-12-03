package org.jkiss.dbeaver.debug.internal.ui;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.jkiss.dbeaver.debug.ui.LaunchContributionFactory;
import org.jkiss.dbeaver.debug.ui.LaunchContributionItem;
import org.jkiss.dbeaver.debug.ui.DebugUi;

public class DebugContributionFactory extends LaunchContributionFactory {

    public DebugContributionFactory() {
        super(DebugUi.DEBUG_AS_MENU_ID);
        setText("Debug As");
        setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG));
    }

    @Override
    protected LaunchContributionItem createContributionItem()
    {
        return new DebugContributionItem();
    }
}
