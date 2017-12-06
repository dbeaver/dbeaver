package org.jkiss.dbeaver.debug.ui;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IIdentifier;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.menus.ExtensionContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.services.IServiceLocator;

public abstract class LaunchContributionFactory extends ExtensionContributionFactory {

    private final String id;

    private String text;
    private ImageDescriptor imageDescriptor;

    public LaunchContributionFactory(String id) {
        this.id = id;
    }

    @Override
    public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
        IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
        String identifierId = createContributionIdentifier();
        IIdentifier identifier = activitySupport.getActivityManager().getIdentifier(identifierId);
        if (!identifier.isEnabled()) {
            return;
        }
        MenuManager menuManager = new MenuManager(text, imageDescriptor, id);
        LaunchContributionItem item = createContributionItem();
        item.setVisible(true);
        menuManager.add(item);
        additions.addContributionItem(menuManager, null);
    }

    public String getText()
    {
        return text;
    }
    
    public void setText(String text)
    {
        this.text = text;
    }

    public ImageDescriptor getImageDescriptor()
    {
        return imageDescriptor;
    }

    public void setImageDescriptor(ImageDescriptor imageDescriptor)
    {
        this.imageDescriptor = imageDescriptor;
    }

    protected String createContributionIdentifier()
    {
        return DebugUi.BUNDLE_SYMBOLIC_NAME + '/' + id;
    }

    protected abstract LaunchContributionItem createContributionItem();
}
