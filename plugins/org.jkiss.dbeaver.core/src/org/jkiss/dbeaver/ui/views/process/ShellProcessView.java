/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;

public class ShellProcessView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.shellProcess";

    private Text processLog;

    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);

        processLog = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        UIUtils.setHelp(group, IHelpContextIds.CTX_QUERY_MANAGER);
    }

    public void setFocus()
    {
        if (processLog != null && !processLog.isDisposed()) {
            processLog.setFocus();
        }
    }

}
