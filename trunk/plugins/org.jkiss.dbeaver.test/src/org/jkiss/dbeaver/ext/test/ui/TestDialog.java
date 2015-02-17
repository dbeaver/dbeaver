/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ext.test.ui;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class TestDialog extends TrayDialog {

    public TestDialog(Shell shell)
    {
        super(shell);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Test");
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

/*
        Composite propsGroup = new Composite(group, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);
*/
        return group;
    }

}
