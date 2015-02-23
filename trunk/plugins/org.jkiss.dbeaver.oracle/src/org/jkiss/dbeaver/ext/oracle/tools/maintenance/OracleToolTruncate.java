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
package org.jkiss.dbeaver.ext.oracle.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleTable;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table truncate
 */
public class OracleToolTruncate implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<OracleTable> tables = CommonUtils.filterCollection(objects, OracleTable.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends OracleMaintenanceDialog<OracleTable> {

        private Button reuseStorage;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<OracleTable> selectedTables)
        {
            super(partSite, "Truncate table(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, OracleTable object) {
            String sql = "TRUNCATE TABLE " + object.getFullQualifiedName();
            if (reuseStorage.getSelection()) {
                sql += " REUSE STORAGE";
            }
            lines.add(sql);
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            reuseStorage = UIUtils.createCheckbox(optionsGroup, "Reuse storage", false);
            reuseStorage.addSelectionListener(SQL_CHANGE_LISTENER);
            reuseStorage.setToolTipText(
                "Specify REUSE STORAGE to retain the space from the deleted rows allocated to the table.\n" +
                "Storage values are not reset to the values when the table or cluster was created.\n" +
                "This space can subsequently be used only by new data in the table or cluster resulting from insert or update operations.\n" +
                "This clause leaves storage parameters at their current settings.");

            createObjectsSelector(parent);
        }
    }

}
