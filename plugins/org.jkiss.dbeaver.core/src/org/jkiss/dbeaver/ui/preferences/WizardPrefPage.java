/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
        super(preferencePage.getTitle());
        this.preferencePage = preferencePage;
        setTitle(title);
        setDescription(description);
    }

    @Override
    public boolean isPageComplete()
    {
        return preferencePage.isValid();
    }

    @Override
    public void createControl(Composite parent)
    {
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
