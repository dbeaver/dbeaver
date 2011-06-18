/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.jkiss.utils.CommonUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;


class ProjectCreateWizardPageSettings extends WizardPage {

    private ProjectCreateData createData;

    protected ProjectCreateWizardPageSettings(ProjectCreateData importData)
    {
        super("Create new project");
        this.createData = importData;

        setTitle("Create new project");
        setDescription("Set project settings.");
    }

    @Override
    public boolean isPageComplete()
    {
        return !CommonUtils.isEmpty(createData.getName());
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, "General", 2, GridData.FILL_HORIZONTAL, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, "Name", "");
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                createData.setName(projectNameText.getText());
                updateState();
            }
        });

        final Text projectDescriptionText = UIUtils.createLabelText(configGroup, "Description", "");
        projectDescriptionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                createData.setDescription(projectDescriptionText.getText());
            }
        });

        setControl(placeholder);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

}
