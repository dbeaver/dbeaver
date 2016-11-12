/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.tools.maintenance;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreObject;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.IExternalTool;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Table truncate
 */
public class PostgreToolTruncate implements IExternalTool
{
    @Override
    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException
    {
        List<PostgreTableBase> tables = CommonUtils.filterCollection(objects, PostgreTableBase.class);
        if (!tables.isEmpty()) {
            SQLDialog dialog = new SQLDialog(activePart.getSite(), tables);
            dialog.open();
        }
    }

    static class SQLDialog extends TableToolDialog {

        private Button onlyCheck;
        private Button restartIdentityCheck;
        private Button cascadeCheck;

        public SQLDialog(IWorkbenchPartSite partSite, Collection<PostgreTableBase> selectedTables)
        {
            super(partSite, "Truncate table(s)", selectedTables);
        }

        @Override
        protected void generateObjectCommand(List<String> lines, PostgreObject object) {
            if (object instanceof PostgreTableBase) {
                String sql = "TRUNCATE TABLE";
                if (onlyCheck.getSelection()) sql += " ONLY";
                sql += " " + ((PostgreTableBase) object).getFullyQualifiedName(DBPEvaluationContext.DDL);
                if (restartIdentityCheck.getSelection())
                    sql += " RESTART IDENTITY";
                else
                    sql += " CONTINUE IDENTITY";
                if (cascadeCheck.getSelection())
                    sql += " CASCADE";
                else
                    sql += " RESTRICT";
                lines.add(sql);
            }
        }

        @Override
        protected void createControls(Composite parent) {
            Group optionsGroup = UIUtils.createControlGroup(parent, "Options", 1, 0, 0);
            optionsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            onlyCheck = UIUtils.createCheckbox(optionsGroup, "Only", "If ONLY is not specified, the table and all its descendant tables (if any) are truncated.", false, 0);
            onlyCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            restartIdentityCheck = UIUtils.createCheckbox(optionsGroup, "Restart identity", "Automatically restart sequences owned by columns of the truncated table(s).", false, 0);
            restartIdentityCheck.addSelectionListener(SQL_CHANGE_LISTENER);
            cascadeCheck = UIUtils.createCheckbox(optionsGroup, "Cascade", "Automatically truncate all tables that have foreign-key references to any of the named tables, or to any tables added to the group due to CASCADE.", false, 0);
            cascadeCheck.addSelectionListener(SQL_CHANGE_LISTENER);

            createObjectsSelector(parent);
        }
    }

}
