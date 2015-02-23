/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ext.mysql.tools.maintenance;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.GenerateMultiSQLDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table check
 */
public class MySQLToolCheck implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<MySQLTable> tables = CommonUtils.filterCollection(objects, MySQLTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Combo optionCombo;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<MySQLTable> selectedTables)
        {
            super(partSite, "Check table(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, MySQLTable object) {
            String sql = "CHECK TABLE " + object.getFullQualifiedName();
            String option = optionCombo.getText();
            if (!CommonUtils.isEmpty(option)) sql += " " + option;
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            optionCombo = UIUtils.createLabelCombo(optionsGroup, "Option", SWT.DROP_DOWN | SWT.READ_ONLY);
            optionCombo.add("");
            optionCombo.add("FOR UPGRADE");
            optionCombo.add("QUICK");
            optionCombo.add("FAST");
            optionCombo.add("MEDIUM");
            optionCombo.add("EXTENDED");
            optionCombo.add("CHANGED");
            optionCombo.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
