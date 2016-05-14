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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.registry.ResourceHandlerDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * PrefPageConnectionTypes
 */
public class PrefPageProjectSettings extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.projectSettings"; //$NON-NLS-1$

    private IProject project;
    private Table resourceTable;
    private TableEditor handlerTableEditor;

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected Control createContents(final Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            UIUtils.createControlLabel(composite, "Resource locations");

            resourceTable = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            resourceTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resourceTable.setHeaderVisible(true);
            resourceTable.setLinesVisible(true);
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, "Resource");
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, "Folder");
            resourceTable.setHeaderVisible(true);
            resourceTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            handlerTableEditor = new TableEditor(resourceTable);
            handlerTableEditor.verticalAlignment = SWT.TOP;
            handlerTableEditor.horizontalAlignment = SWT.RIGHT;
            handlerTableEditor.grabHorizontal = true;
            handlerTableEditor.grabVertical = true;
            resourceTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e)
                {
                    disposeOldEditor();

                    TableItem item = resourceTable.getItem(new Point(0, e.y));
                    if (item == null) {
                        return;
                    }
                    int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
                    if (columnIndex <= 0) {
                        return;
                    }
                    if (columnIndex == 1) {
                        ResourceSelectionDialog dialog = new ResourceSelectionDialog(resourceTable.getShell(), project, "Select " + item.getText(0) + " root folder");
                        dialog.open();
                        final Text editor = new Text(resourceTable, SWT.NONE);
                        editor.setText(item.getText(1));
                        editor.selectAll();
                        handlerTableEditor.setEditor(editor, item, 1);
                        editor.setFocus();
                    }
                }
            });
        }

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
        resourceTable.removeAll();
        for (ResourceHandlerDescriptor descriptor : DBeaverCore.getInstance().getProjectRegistry().getResourceHandlers()) {
            if (!descriptor.isManagable()) {
                continue;
            }
            TableItem item = new TableItem(resourceTable, SWT.NONE);
            item.setData(descriptor);
            item.setText(0, descriptor.getName());

            if (descriptor.getDefaultRoot() != null) {
                item.setText(1, descriptor.getDefaultRoot());
            }
        }
        UIUtils.packColumns(resourceTable, true);

        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        return super.performOk();
    }

    @Override
    public IAdaptable getElement()
    {
        return project;
    }

    @Override
    public void setElement(IAdaptable element)
    {
        if (element instanceof IProject) {
            this.project = (IProject) element;
        } else {
            this.project = DBUtils.getAdapter(IProject.class, element);
        }
    }

}
