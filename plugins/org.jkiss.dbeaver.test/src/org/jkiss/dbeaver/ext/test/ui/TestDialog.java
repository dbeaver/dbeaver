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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.vtabs.ITabItem;
import org.jkiss.dbeaver.ui.controls.vtabs.TabbedPropertyList;

public class TestDialog extends TrayDialog {

    private ITabItem[] tabs;

    public TestDialog(Shell shell)
    {
        super(shell);

        tabs = new ITabItem[2];
        tabs[0] = new TabItemImpl("Tab 1", DBIcon.TREE_TABLE.getImage());
        tabs[1] = new TabItemImpl("Tab with long-long name", DBIcon.TREE_COLUMNS.getImage());
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Test");
        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        Composite sheet = UIUtils.createPlaceholder(group, 2);
        sheet.setLayoutData(new GridData(GridData.FILL_BOTH));

        TabbedPropertyList tabbedPropertyList = new TabbedPropertyList(sheet);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 300;
        gd.widthHint = 500;
        tabbedPropertyList.setLayoutData(gd);
        tabbedPropertyList.setElements(tabs);
        tabbedPropertyList.select(0);

        Text pane = new Text(sheet, SWT.NONE);
        pane.setLayoutData(new GridData(GridData.FILL_BOTH));
        //pane.setLayout(new FillLayout());

/*
        Composite propsGroup = new Composite(group, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);
*/
        return group;
    }

    private static class TabItemImpl implements ITabItem {
        private String text;
        private Image image;

        private TabItemImpl(String text, Image image) {
            this.text = text;
            this.image = image;
        }

        @Override
        public Image getImage() {
            return image;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public boolean isIndented() {
            return false;
        }

        @Override
        public Composite createControl(Composite parent) {
            return new Composite(parent, SWT.BORDER);
        }
    }
}
