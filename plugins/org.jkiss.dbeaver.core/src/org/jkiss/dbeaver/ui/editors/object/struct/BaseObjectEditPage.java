/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

public abstract class BaseObjectEditPage extends DialogPage {

    private EditObjectDialog container;

    public BaseObjectEditPage(String title)
    {
        super(title);
    }

    public BaseObjectEditPage(String title, DBIcon icon) {
        super(title, DBeaverIcons.getImageDescriptor(icon));
    }

    @Override
    public void performHelp() {
        super.performHelp();
    }

    @Override
    public final void createControl(Composite parent) {
        Control pageContents = createPageContents(parent);
        setControl(pageContents);
        pageContents.addHelpListener(new HelpListener() {
            @Override
            public void helpRequested(HelpEvent e) {
                performHelp();
            }
        });
    }


    public boolean isPageComplete() {
        return true;
    }

    protected void updatePageState() {
        if (container != null) {
            container.updateButtons();
        }
    }

    void setContainer(EditObjectDialog container) {
        this.container = container;
    }

    protected void performFinish() throws DBException {

    }

    protected abstract Control createPageContents(Composite parent);

    public boolean edit() {
        return EditObjectDialog.showDialog(this);
    }

    public boolean edit(Shell shell) {
        return EditObjectDialog.showDialog(shell, this);
    }
}
