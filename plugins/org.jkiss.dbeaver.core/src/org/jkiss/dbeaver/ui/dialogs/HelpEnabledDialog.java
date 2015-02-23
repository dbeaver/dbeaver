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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Dialog with enabled help
 */
public abstract class HelpEnabledDialog extends TrayDialog {

    protected final String helpContextID;

    protected HelpEnabledDialog(Shell shell, String helpContextID)
    {
        super(shell);
        this.helpContextID = helpContextID;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        final Control contents = super.createContents(parent);
        UIUtils.setHelp(contents, helpContextID);
        return contents;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

//    protected Button createHelpButton(Composite parent)
//    {
//        final Button button = createButton(parent, IDialogConstants.HELP_ID, IDialogConstants.HELP_LABEL, false);
//        button.setImage(
//            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_LCL_LINKTO_HELP)
//        );
//        return button;
//    }

}
