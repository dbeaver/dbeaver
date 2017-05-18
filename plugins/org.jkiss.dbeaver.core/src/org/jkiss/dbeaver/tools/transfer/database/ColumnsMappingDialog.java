/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ColumnsMappingDialog
 */
public class ColumnsMappingDialog extends StatusDialog {

    private final DatabaseConsumerSettings settings;
    private final DatabaseMappingContainer mapping;
    private final Collection<DatabaseMappingAttribute> attributeMappings;
    private TableViewer mappingViewer;
    private Font boldFont;

    public ColumnsMappingDialog(DataTransferWizard wizard, DatabaseConsumerSettings settings, DatabaseMappingContainer mapping)
    {
        super(wizard.getShell());
        this.settings = settings;
        this.mapping = mapping;
        attributeMappings = mapping.getAttributeMappings(wizard.getContainer());
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        DBPDataSource targetDataSource = settings.getTargetDataSource(mapping);

        getShell().setText("Map columns of " + mapping.getTargetName());
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        new Label(composite, SWT.NONE).setText("Source entity: " + DBUtils.getObjectFullName(mapping.getSource(), DBPEvaluationContext.UI) +
            " [" + mapping.getSource().getDataSource().getContainer().getName() + "]");
        new Label(composite, SWT.NONE).setText("Target entity: " + mapping.getTargetName() +
            " [" + (targetDataSource == null ? "?" : targetDataSource.getContainer().getName()) + "]");
        mappingViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 600;
        gd.heightHint = 300;
        mappingViewer.getTable().setLayoutData(gd);
        mappingViewer.getTable().setLinesVisible(true);
        mappingViewer.getTable().setHeaderVisible(true);
        mappingViewer.setContentProvider(new ListContentProvider());
        mappingViewer.getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.DEL) {
                    for (TableItem item : mappingViewer.getTable().getSelection()) {
                        DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                        attribute.setMappingType(DatabaseMappingType.skip);
                    }
                    updateStatus(Status.OK_STATUS);
                    mappingViewer.refresh();
                } else if (e.character == SWT.SPACE) {
                    for (TableItem item : mappingViewer.getTable().getSelection()) {
                        DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                        attribute.setMappingType(DatabaseMappingType.existing);
                        try {
                            attribute.updateMappingType(new VoidProgressMonitor());
                        } catch (DBException e1) {
                            updateStatus(GeneralUtils.makeExceptionStatus(e1));
                        }
                    }
                    mappingViewer.refresh();
                }
            }
        });

        {
            TableViewerColumn columnSource = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnSource.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) cell.getElement();
                    cell.setText(DBUtils.getObjectFullName(attrMapping.getSource(), DBPEvaluationContext.UI));
                    if (attrMapping.getIcon() != null) {
                        cell.setImage(DBeaverIcons.getImage(attrMapping.getIcon()));
                    }
                }
            });
            columnSource.getColumn().setText("Source Column");
            columnSource.getColumn().setWidth(170);
        }
        {
            TableViewerColumn columnSourceType = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnSourceType.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    cell.setText(((DatabaseMappingAttribute) cell.getElement()).getSourceType());
                }
            });
            columnSourceType.getColumn().setText("Source Type");
            columnSourceType.getColumn().setWidth(100);
        }

        {
            TableViewerColumn columnTarget = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnTarget.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) cell.getElement();
                    cell.setText(mapping.getTargetName());
                    if (mapping.mappingType == DatabaseMappingType.unspecified) {
                        cell.setBackground(DBeaverUI.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                    } else {
                        cell.setBackground(null);
                    }
                    cell.setFont(boldFont);
                }
            });
            columnTarget.getColumn().setText("Target Column");
            columnTarget.getColumn().setWidth(170);
            columnTarget.setEditingSupport(new EditingSupport(mappingViewer) {
                @Override
                protected CellEditor getCellEditor(Object element)
                {
                    try {
                        java.util.List<String> items = new ArrayList<>();
                        DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) element;
                        if (mapping.getParent().getMappingType() == DatabaseMappingType.existing &&
                            mapping.getParent().getTarget() instanceof DBSEntity)
                        {
                            DBSEntity parentEntity = (DBSEntity)mapping.getParent().getTarget();
                            for (DBSEntityAttribute attr : CommonUtils.safeCollection(parentEntity.getAttributes(new VoidProgressMonitor()))) {
                                items.add(attr.getName());
                            }
                        }

                        items.add(DatabaseMappingAttribute.TARGET_NAME_SKIP);
                        CustomComboBoxCellEditor editor = new CustomComboBoxCellEditor(
                            mappingViewer.getTable(),
                            items.toArray(new String[items.size()]),
                            SWT.DROP_DOWN);
                        updateStatus(Status.OK_STATUS);
                        return editor;
                    } catch (DBException e) {
                        updateStatus(GeneralUtils.makeExceptionStatus(e));
                        return null;
                    }
                }

                @Override
                protected boolean canEdit(Object element)
                {
                    return true;
                }

                @Override
                protected Object getValue(Object element)
                {
                    return ((DatabaseMappingAttribute)element).getTargetName();
                }

                @Override
                protected void setValue(Object element, Object value)
                {
                    try {
                        String name = CommonUtils.toString(value);
                        DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                        if (DatabaseMappingAttribute.TARGET_NAME_SKIP.equals(name)) {
                            attrMapping.setMappingType(DatabaseMappingType.skip);
                        } else {
                            if (attrMapping.getParent().getMappingType() == DatabaseMappingType.existing &&
                                attrMapping.getParent().getTarget() instanceof DBSEntity)
                            {
                                DBSEntity parentEntity = (DBSEntity)attrMapping.getParent().getTarget();
                                for (DBSEntityAttribute attr : CommonUtils.safeCollection(parentEntity.getAttributes(new VoidProgressMonitor()))) {
                                    if (name.equalsIgnoreCase(attr.getName())) {
                                        attrMapping.setTarget(attr);
                                        attrMapping.setMappingType(DatabaseMappingType.existing);
                                        return;
                                    }
                                }
                            }
                            attrMapping.setMappingType(DatabaseMappingType.create);
                            attrMapping.setTargetName(name);
                        }
                        updateStatus(Status.OK_STATUS);
                    } catch (DBException e) {
                        updateStatus(GeneralUtils.makeExceptionStatus(e));
                    } finally {
                        mappingViewer.refresh();
                    }
                }
            });
        }

        {
            TableViewerColumn columnTargetType = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnTargetType.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) cell.getElement();
                    DBPDataSource dataSource = settings.getTargetDataSource(attrMapping);
                    cell.setText(attrMapping.getTargetType(dataSource));
                    cell.setFont(boldFont);
                }
            });
            columnTargetType.getColumn().setText("Target Type");
            columnTargetType.getColumn().setWidth(100);
            columnTargetType.setEditingSupport(new EditingSupport(mappingViewer) {
                @Override
                protected CellEditor getCellEditor(Object element)
                {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;

                    Set<String> types = new LinkedHashSet<>();
                    DBPDataSource dataSource = settings.getTargetDataSource(attrMapping);
                    if (dataSource instanceof DBPDataTypeProvider) {
                        for (DBSDataType type : ((DBPDataTypeProvider) dataSource).getLocalDataTypes()) {
                            types.add(type.getName());
                        }
                    }
                    types.add(attrMapping.getTargetType(dataSource));

                    return new CustomComboBoxCellEditor(mappingViewer.getTable(), types.toArray(new String[types.size()]), SWT.BORDER);
                }
                @Override
                protected boolean canEdit(Object element)
                {
                    return true;
                }
                @Override
                protected Object getValue(Object element)
                {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                    return attrMapping.getTargetType(settings.getTargetDataSource(attrMapping));
                }
                @Override
                protected void setValue(Object element, Object value)
                {
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                    attrMapping.setTargetType(CommonUtils.toString(value));
                    mappingViewer.refresh(element);
                }
            });
        }

        {
            TableViewerColumn columnType = new TableViewerColumn(mappingViewer, SWT.LEFT);
            columnType.setLabelProvider(new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell)
                {
                    DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) cell.getElement();
                    String text = "";
                    switch (mapping.getMappingType()) {
                        case unspecified: text = "?"; break;
                        case existing: text = "existing"; break;
                        case create: text = "new"; break;
                        case skip: text = "skip"; break;
                    }
                    cell.setText(text);
                }
            });
            columnType.getColumn().setText("Mapping");
            columnType.getColumn().setWidth(60);
        }

        mappingViewer.setInput(attributeMappings);

        return parent;
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    @Override
    public boolean close()
    {
        UIUtils.dispose(boldFont);
        return super.close();
    }
}
