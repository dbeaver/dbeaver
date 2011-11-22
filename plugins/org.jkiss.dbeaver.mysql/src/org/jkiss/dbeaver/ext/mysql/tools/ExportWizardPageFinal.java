/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;


class ExportWizardPageFinal extends WizardPage {

    private StyledText dumpLogText;

    protected ExportWizardPageFinal()
    {
        super("Export progress");
        setTitle("Export progress");
        setDescription("Database export progress log");
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        dumpLogText = new StyledText(composite, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        dumpLogText.setLayoutData(gd);

        setControl(composite);
    }

    public void appendLog(final String line)
    {
        if (getShell().isDisposed()) {
            return;
        }
        UIUtils.runInUI(getShell(), new Runnable() {
            public void run()
            {
                if (!dumpLogText.isDisposed()) {
                    dumpLogText.append(line);
                    //dumpLogText.append(ContentUtils.getDefaultLineSeparator());
                    dumpLogText.setCaretOffset(dumpLogText.getCharCount());
                    dumpLogText.showSelection();
                }
            }
        });
    }

}
