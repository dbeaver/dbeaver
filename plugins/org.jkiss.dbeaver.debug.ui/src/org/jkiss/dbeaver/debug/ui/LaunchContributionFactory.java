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
        return DebugUI.BUNDLE_SYMBOLIC_NAME + '/' + id;
    }

    protected abstract LaunchContributionItem createContributionItem();
}
