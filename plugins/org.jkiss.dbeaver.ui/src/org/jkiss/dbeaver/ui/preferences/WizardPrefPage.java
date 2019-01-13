/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference page adapter for wizard page
 */
public class WizardPrefPage extends WizardPage implements ICompositeDialogPage {

    private final IPreferencePage preferencePage;
    private final List<WizardPrefPage> subPages = new ArrayList<>();

    public WizardPrefPage(IPreferencePage preferencePage, String title, String description)
    {
        super(preferencePage.getClass().getName());
        this.preferencePage = preferencePage;
        setTitle(title);
        setDescription(description);
    }

    @Override
    public boolean isPageComplete()
    {
        return getControl() == null || preferencePage.isValid();
    }

    @Override
    public void createControl(Composite parent)
    {
        if (preferencePage instanceof AbstractPrefPage) {
            ((AbstractPrefPage) preferencePage).disableButtons();
        }
        preferencePage.createControl(parent);
    }

    @Override
    public void dispose()
    {
        preferencePage.dispose();
        super.dispose();
    }

    @Override
    public Control getControl()
    {
        return preferencePage.getControl();
    }

    @Override
    public String getDescription()
    {
        if (!CommonUtils.isEmpty(preferencePage.getDescription())) {
            return preferencePage.getDescription();
        }
        return super.getDescription();
    }

    @Override
    public String getErrorMessage()
    {
        return preferencePage.getErrorMessage();
    }

    @Override
    public Image getImage()
    {
        return preferencePage.getImage();
    }

    @Override
    public String getMessage()
    {
        return preferencePage.getMessage();
    }

    @Override
    public String getTitle()
    {
        if (!CommonUtils.isEmpty(preferencePage.getTitle())) {
            return preferencePage.getTitle();
        }
        return super.getTitle();
    }

    @Override
    public void performHelp()
    {
        preferencePage.performHelp();
    }

    @Override
    public void setVisible(boolean visible)
    {
        preferencePage.setVisible(visible);
    }

    public void performFinish()
    {
        preferencePage.performOk();
    }

    public void performCancel()
    {
        preferencePage.performCancel();
    }

    @Override
    public WizardPrefPage[] getSubPages() {
        if (subPages.isEmpty()) {
            return null;
        }
        return subPages.toArray(new WizardPrefPage[subPages.size()]);
    }

    public void addSubPage(IPreferencePage page, String title, String description) {
        subPages.add(new WizardPrefPage(page, title, description));
        // Sety the same element to sub page
        if (preferencePage instanceof IWorkbenchPropertyPage && page instanceof IWorkbenchPropertyPage) {
            ((IWorkbenchPropertyPage) page).setElement(((IWorkbenchPropertyPage) preferencePage).getElement());
        }
    }

}
