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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;


class MySQLExportWizardPageObjects extends MySQLWizardPageSettings<MySQLExportWizard>
{

    private Tree catalogTree;
    private Tree tableTree;

    protected MySQLExportWizardPageObjects(MySQLExportWizard wizard)
    {
        super(wizard, "Schemas/tables");
        setTitle("Choose objects to export");
        setDescription("Schemas/tables/views which will be exported");
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group objectsGroup = UIUtils.createControlGroup(composite, MySQLMessages.tools_db_export_wizard_page_settings_group_objects, 3, GridData.FILL_HORIZONTAL, 0);
        final MySQLDataSource dataSource = wizard.getDatabaseObject().getDataSource();
        final DBNDatabaseNode dsNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dataSource.getContainer());
        DatabaseNavigatorTree tree = new DatabaseNavigatorTree(objectsGroup, dsNode, SWT.BORDER | SWT.CHECK, false);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        setControl(composite);
    }

    private void updateState()
    {
        //wizard.removeDefiner = removeDefiner.getSelection();

        getContainer().updateButtons();
    }

}
