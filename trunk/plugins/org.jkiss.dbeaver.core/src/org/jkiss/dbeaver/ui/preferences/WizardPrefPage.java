/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.utils.CommonUtils;

/**
 * Preference page adapter for wizard page
 */
public class WizardPrefPage extends WizardPage {

    private final IPreferencePage preferencePage;

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

}
