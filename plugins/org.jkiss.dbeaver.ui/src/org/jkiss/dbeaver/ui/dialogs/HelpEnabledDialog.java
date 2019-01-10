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
