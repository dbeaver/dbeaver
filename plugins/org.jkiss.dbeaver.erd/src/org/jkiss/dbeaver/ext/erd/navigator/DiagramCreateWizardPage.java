/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ui.UIUtils;


class DiagramCreateWizardPage extends WizardPage {

    private EntityDiagram diagram;

    protected DiagramCreateWizardPage(EntityDiagram diagram)
    {
        super("Create new diagram");
        this.diagram = diagram;

        setTitle("Create new diagram");
        setDescription("Manage diagram content.");
    }

    @Override
    public boolean isPageComplete()
    {
        return !CommonUtils.isEmpty(diagram.getName());
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        Composite configGroup = UIUtils.createControlGroup(placeholder, "General", 2, GridData.FILL_HORIZONTAL, 0);

        final Text projectNameText = UIUtils.createLabelText(configGroup, "Name", "");
        projectNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                diagram.setName(projectNameText.getText());
                updateState();
            }
        });

        setControl(placeholder);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

}
