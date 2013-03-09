/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;

/**
 * ColumnsMappingDialog
 */
public class ColumnsMappingDialog extends Dialog {

    private final DatabaseConsumerSettings.ContainerMapping mapping;
    private TableViewer mappingViewer;

    private static abstract class MappingLabelProvider extends CellLabelProvider {
        @Override
        public void update(ViewerCell cell)
        {
            DatabaseConsumerSettings.AttributeMapping mapping = (DatabaseConsumerSettings.AttributeMapping) cell.getElement();
            if (mapping.mappingType == DatabaseConsumerSettings.MappingType.unspecified) {
                cell.setBackground(DBeaverUI.getSharedTextColors().getColor(SharedTextColors.COLOR_BACK_DELETED));
            } else {
                cell.setBackground(null);
            }
        }
    }

    public ColumnsMappingDialog(Shell parentShell, DatabaseConsumerSettings.ContainerMapping mapping)
    {
        super(parentShell);
        this.mapping = mapping;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Map columns of " + mapping.getTargetName());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        new Label(composite, SWT.NONE).setText("Source entity: " + DBUtils.getObjectFullName(mapping.source));
        new Label(composite, SWT.NONE).setText("Target entity: " + mapping.getTargetName());
        mappingViewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        gd.heightHint = 300;
        mappingViewer.getTable().setLayoutData(gd);
        mappingViewer.getTable().setLinesVisible(true);
        mappingViewer.getTable().setHeaderVisible(true);
        mappingViewer.setContentProvider(new ListContentProvider());

        TableViewerColumn columnSource = new TableViewerColumn(mappingViewer, SWT.LEFT);
        columnSource.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseConsumerSettings.AttributeMapping mapping = (DatabaseConsumerSettings.AttributeMapping) cell.getElement();
                cell.setText(DBUtils.getObjectFullName(mapping.source));
                super.update(cell);
            }
        });
        columnSource.getColumn().setText("Source");
        columnSource.getColumn().setWidth(170);

        TableViewerColumn columnTarget = new TableViewerColumn(mappingViewer, SWT.LEFT);
        columnTarget.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseConsumerSettings.AttributeMapping mapping = (DatabaseConsumerSettings.AttributeMapping) cell.getElement();
                cell.setText(mapping.getTargetName());
                super.update(cell);
            }
        });
        columnTarget.getColumn().setText("Target");
        columnTarget.getColumn().setWidth(170);

        TableViewerColumn columnType = new TableViewerColumn(mappingViewer, SWT.LEFT);
        columnType.setLabelProvider(new MappingLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DatabaseConsumerSettings.ContainerMapping mapping = (DatabaseConsumerSettings.ContainerMapping) cell.getElement();
                String text = "";
                switch (mapping.mappingType) {
                    case unspecified: text = "?"; break;
                    case existing: text = "table"; break;
                    case create: text = "new"; break;
                    case skip: text = "skip"; break;
                }
                cell.setText(text);
                super.update(cell);
            }
        });
        columnType.getColumn().setText("Type");
        columnType.getColumn().setWidth(60);

        mappingViewer.setInput(mapping.attributeMappings.values());

        return parent;
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

}
