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

package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * EditDictionaryDialog
 *
 * @author Serge Rider
 */
public class EditDictionaryDialog extends AttributesSelectorDialog {

    private Text criteriaText;
    private DBVEntity dictionary;
    private Collection<DBSEntityAttribute> descColumns;
    private DBSEntity entity;

    public EditDictionaryDialog(
        Shell shell,
        String title,
        final DBSEntity entity)
    {
        super(shell, title, entity);
        this.entity = entity;
        this.dictionary = DBVUtils.findVirtualEntity(entity, true);
        DBeaverUI.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                try {
                    if (dictionary.getDescriptionColumnNames() == null) {
                        Collection<? extends DBSEntityAttribute> tablePK = DBUtils.getBestTableIdentifier(monitor, entity);
                        if (tablePK != null && !tablePK.isEmpty()) {
                            dictionary.setDescriptionColumnNames(DBVEntity.getDefaultDescriptionColumn(monitor, tablePK.iterator().next()));
                        }
                    }
                    descColumns = dictionary.getDescriptionColumns(monitor, entity);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

    public DBVEntity getDictionary()
    {
        return dictionary;
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        Label label = UIUtils.createControlLabel(panel,
            "Choose dictionary description columns or set custom criteria");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
    }

    @Override
    protected void createContentsAfterColumns(Composite panel)
    {
        Group group = UIUtils.createControlGroup(panel, "Custom criteria", 1, GridData.FILL_HORIZONTAL, 0);
        criteriaText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 50;
        criteriaText.setLayoutData(gd);
        if (!CommonUtils.isEmpty(dictionary.getDescriptionColumnNames())) {
            criteriaText.setText(dictionary.getDescriptionColumnNames());
        }
    }

    @Override
    public boolean isColumnSelected(DBSEntityAttribute attribute)
    {
        return descColumns.contains(attribute);
    }

    @Override
    protected void handleColumnsChange() {
        descColumns = getSelectedAttributes();
        StringBuilder custom = new StringBuilder();
        for (DBSEntityAttribute column : descColumns) {
            if (custom.length() > 0) {
                custom.append(",");
            }
            custom.append(DBUtils.getQuotedIdentifier(column));
        }
        criteriaText.setText(custom.toString());
    }

    @Override
    protected void okPressed()
    {
        dictionary.setDescriptionColumnNames(criteriaText.getText());
        entity.getDataSource().getContainer().persistConfiguration();
        super.okPressed();
    }

}
