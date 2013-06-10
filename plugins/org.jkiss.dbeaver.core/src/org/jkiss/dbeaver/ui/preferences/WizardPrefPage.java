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
}
