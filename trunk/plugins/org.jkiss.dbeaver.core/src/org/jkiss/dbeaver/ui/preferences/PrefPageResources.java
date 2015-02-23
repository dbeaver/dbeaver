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
package org.jkiss.dbeaver.ui.preferences;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.registry.ResourceHandlerDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageResources
 */
public class PrefPageResources extends PreferencePage implements IWorkbenchPreferencePage
{
    static final Log log = Log.getLog(PrefPageResources.class);

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.resources"; //$NON-NLS-1$

    private Table handlerTable;
    private TableEditor handlerTableEditor;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        Group groupRoots = UIUtils.createControlGroup(composite, "Root directories", 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
        groupRoots.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        handlerTable = new Table(groupRoots, SWT.SINGLE | SWT.BORDER);
        handlerTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        handlerTable.setHeaderVisible(true);
        handlerTable.setLinesVisible(true);
        UIUtils.createTableColumn(handlerTable, SWT.LEFT, "Resource type");
        UIUtils.createTableColumn(handlerTable, SWT.LEFT, "Root folder");
        UIUtils.packColumns(handlerTable, true);
        handlerTableEditor = new TableEditor(handlerTable);
        handlerTableEditor.verticalAlignment = SWT.TOP;
        handlerTableEditor.horizontalAlignment = SWT.RIGHT;
        handlerTableEditor.grabHorizontal = true;
        handlerTableEditor.grabVertical = true;
        handlerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e)
            {
                disposeOldEditor();

                TableItem item = handlerTable.getItem(new Point(0, e.y));
                if (item == null) {
                    return;
                }
                int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
                if (columnIndex <= 0) {
                    return;
                }
                if (columnIndex == 1) {
                    handlerTableEditor.setEditor(new Text(handlerTable, SWT.NONE), item, 1);
                }
            }
        });

        performDefaults();

        return composite;
    }

    private void disposeOldEditor()
    {
        Control oldEditor = handlerTableEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    @Override
    protected void performDefaults()
    {
        handlerTable.removeAll();
        for (ResourceHandlerDescriptor descriptor : DBeaverCore.getInstance().getProjectRegistry().getResourceHandlers()) {
            if (CommonUtils.isEmpty(descriptor.getContentTypes())) {
                continue;
            }
            IContentType contentType = descriptor.getContentTypes().iterator().next();
            TableItem item = new TableItem(handlerTable, SWT.LEFT);
            item.setText(0, contentType.getName());
            //item.setImage(0, descriptor.getIcon());
            item.setText(1, descriptor.getDefaultRoot());
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        //store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        RuntimeUtils.savePreferenceStore(store);

        return super.performOk();
    }

}