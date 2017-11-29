package org.jkiss.dbeaver.internal.launch.ui;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.jkiss.dbeaver.launch.ui.LaunchContributionFactory;
import org.jkiss.dbeaver.launch.ui.LaunchContributionItem;
import org.jkiss.dbeaver.launch.ui.LaunchUi;

public class DebugContributionFactory extends LaunchContributionFactory {

    public DebugContributionFactory() {
        super(LaunchUi.DEBUG_AS_MENU_ID);
        setText("Debug As");
        setImageDescriptor(DebugUITools.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG));
    }

    @Override
    protected LaunchContributionItem createContributionItem()
    {
        return new DebugContributionItem();
    }
}
