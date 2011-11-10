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
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;


class ProjectCreateWizardPageSettings extends WizardPage {

    private ProjectCreateData createData;

    protected ProjectCreateWizardPageSettings(ProjectCreateData importData)
    {
        super(CoreMessages.dialog_project_create_settings_name);
        this.createData = importData;

        setTitle(CoreMessages.dialog_project_create_settings_title);
        setDescription(CoreMessages.dialog_project_create_settings_description);
    }

    @Override
    public boolean isPageComplete()
    {
        return !CommonUtils.isEmpty(createData.getName());
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, CoreMessages.dialog_project_create_settings_group_general, 2, GridData.FILL_HORIZONTAL, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, CoreMessages.dialog_project_create_settings_label_name, ""); //$NON-NLS-2$
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                createData.setName(projectNameText.getText());
                updateState();
            }
        });

        final Text projectDescriptionText = UIUtils.createLabelText(configGroup, CoreMessages.dialog_project_create_settings_label_description, ""); //$NON-NLS-2$
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
