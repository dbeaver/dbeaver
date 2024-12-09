/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingAttribute;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingType;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferAttributeTransformerDescriptor;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * ColumnsMappingDialog
 */
class ColumnsMappingDialog extends DialogPage {

    private final DatabaseConsumerSettings settings;
    private final DatabaseMappingContainer mapping;
    private final Collection<DatabaseMappingAttribute> attributeMappings;
    private TableViewer mappingViewer;
    private Font boldFont;

    ColumnsMappingDialog(DatabaseConsumerSettings settings, DatabaseMappingContainer mapping) {
        this.settings = settings;
        this.mapping = mapping;
        attributeMappings = mapping.getAttributeMappings();
    }

    @Override
    public void createControl(Composite parent) {
        DBPDataSource targetDataSource = settings.getTargetDataSource(mapping);

        boldFont = UIUtils.makeBoldFont(parent.getFont());
        parent.addDisposeListener(e -> boldFont.dispose());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        DBPDataSource sourceDataSource = mapping.getSource().getDataSource();
        UIUtils.createLabelText(composite, DTUIMessages.columns_mapping_dialog_composite_label_text_source_container,
            sourceDataSource == null ? "" : sourceDataSource.getContainer().getName(), SWT.BORDER | SWT.READ_ONLY);
        Text sourceEntity = UIUtils.createLabelText(composite, DTUIMessages.columns_mapping_dialog_composite_label_text_source_entity, DBUtils.getObjectFullName(mapping.getSource(), DBPEvaluationContext.UI), SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
        ((GridData) sourceEntity.getLayoutData()).widthHint = 600;
        ((GridData) sourceEntity.getLayoutData()).heightHint = UIUtils.getFontHeight(sourceEntity) * 3;
        UIUtils.createLabelText(composite, DTUIMessages.columns_mapping_dialog_composite_label_text_target_container, (targetDataSource == null ? "?" : targetDataSource.getContainer().getName()), SWT.BORDER | SWT.READ_ONLY);
        Text targetEntity = UIUtils.createLabelText(composite, DTUIMessages.columns_mapping_dialog_composite_label_text_target_entity, mapping.getTargetName(), SWT.BORDER | SWT.READ_ONLY);
        ((GridData) targetEntity.getLayoutData()).widthHint = 600;
        ((GridData) targetEntity.getLayoutData()).heightHint = UIUtils.getFontHeight(sourceEntity) * 3;

        mappingViewer = new TableViewer(composite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 600;
        gd.heightHint = 300;
        gd.horizontalSpan = 2;
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
                    mappingViewer.refresh();
                } else if (e.character == SWT.SPACE) {
                    for (TableItem item : mappingViewer.getTable().getSelection()) {
                        DatabaseMappingAttribute attribute = (DatabaseMappingAttribute) item.getData();
                        attribute.setMappingType(DatabaseMappingType.existing);
                        try {
                            attribute.updateMappingType(new VoidProgressMonitor(), false, false);
                        } catch (DBException e1) {
                            DBWorkbench.getPlatformUI().showError("Bad mapping", "Invalid column mapping", e1);
                        }
                    }
                    mappingViewer.refresh();
                }
            }
        });

        final ViewerColumnController<?, ?> columnController = new ViewerColumnController<>(getClass().getName(), mappingViewer);

        columnController.addColumn(DTUIMessages.columns_mapping_dialog_column_source_text, null, SWT.LEFT, true, false, new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) cell.getElement();
                cell.setText(DBUtils.getObjectFullName(mapping.getSource(), DBPEvaluationContext.UI));
                if (mapping.getIcon() != null) {
                    cell.setImage(DBeaverIcons.getImage(mapping.getIcon()));
                }
            }
        });

        columnController.addColumn(DTUIMessages.columns_mapping_dialog_column_source_type_text, null, SWT.LEFT, true, false, new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(((DatabaseMappingAttribute) cell.getElement()).getSourceType());
            }
        });

        columnController.addColumn(DTUIMessages.columns_mapping_dialog_column_target_text, null, SWT.LEFT, true, false, false, null, new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) cell.getElement();
                cell.setText(mapping.getTargetName());
                if (mapping.getMappingType() == DatabaseMappingType.unspecified) {
                    cell.setBackground(UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                } else {
                    cell.setBackground(null);
                }
                cell.setFont(boldFont);
            }
        }, new EditingSupport(mappingViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                try {
                    java.util.List<String> items = new ArrayList<>();
                    DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) element;
                    DatabaseMappingContainer container = mapping.getParent();
                    if ((container.getMappingType() == DatabaseMappingType.existing || container.getMappingType() == DatabaseMappingType.recreate) &&
                        container.getTarget() instanceof DBSEntity) {
                        DBSEntity parentEntity = (DBSEntity) container.getTarget();
                        for (DBSEntityAttribute attr : CommonUtils.safeCollection(parentEntity.getAttributes(new VoidProgressMonitor()))) {
                            items.add(attr.getName());
                        }
                    }

                    items.add(DatabaseMappingAttribute.TARGET_NAME_SKIP);
                    return new CustomComboBoxCellEditor(
                        mappingViewer,
                        mappingViewer.getTable(),
                        items.toArray(new String[0]),
                        SWT.DROP_DOWN);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Bad value", "Wrong target column", e);
                    return null;
                }
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                return ((DatabaseMappingAttribute) element).getTargetName();
            }

            @Override
            protected void setValue(Object element, Object value) {
                try {
                    String name = CommonUtils.toString(value);
                    DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                    if (DatabaseMappingAttribute.TARGET_NAME_SKIP.equals(name)) {
                        attrMapping.setMappingType(DatabaseMappingType.skip);
                    } else {
                        DatabaseMappingContainer container = attrMapping.getParent();
                        if ((container.getMappingType() == DatabaseMappingType.existing || container.getMappingType() == DatabaseMappingType.recreate) &&
                            container.getTarget() instanceof DBSEntity) {
                            DBSEntity parentEntity = (DBSEntity) container.getTarget();
                            for (DBSEntityAttribute attr : CommonUtils.safeCollection(parentEntity.getAttributes(new VoidProgressMonitor()))) {
                                if (name.equalsIgnoreCase(attr.getName())) {
                                    attrMapping.setTarget(attr);
                                    attrMapping.setMappingType(DatabaseMappingType.existing);
                                    attrMapping.setTargetName(name);
                                    return;
                                }
                            }
                        }
                        attrMapping.setMappingType(DatabaseMappingType.create);
                        attrMapping.setTargetName(name);
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Bad value", "Wrong target", e);
                } finally {
                    mappingViewer.refresh();
                }
            }
        });

        columnController.addColumn(DTUIMessages.columns_mapping_dialog_column_target_type_text, null, SWT.LEFT, true, false, false, null, new ColumnLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) cell.getElement();
                DBPDataSource dataSource = settings.getTargetDataSource(attrMapping);
                cell.setText(attrMapping.getTargetType(dataSource, true));
                cell.setFont(boldFont);
            }
        }, new EditingSupport(mappingViewer) {
            @Override
            protected CellEditor getCellEditor(Object element) {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;

                Map<String, String> types = new TreeMap<>();
                DBSObjectContainer container = settings.getContainer();
                if (container != null) {
                    DBPDataSource dataSource = container.getDataSource();
                    if (dataSource != null) {
                        // Also add data types with aliases
                        Collection<String> dataTypes = dataSource.getSQLDialect().getDataTypes(dataSource);
                        if (!CommonUtils.isEmpty(dataTypes)) {
                            for (String dataType : dataTypes) {
                                types.put(dataType.toUpperCase(Locale.ROOT), dataType);
                            }
                        }
                    }
                }
                DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, container);
                if (dataTypeProvider != null) {
                    for (DBSDataType type : dataTypeProvider.getLocalDataTypes()) {
                        types.put(type.getName().toUpperCase(Locale.ROOT), type.getName());
                    }
                }
                String targetType = attrMapping.getTargetType(settings.getTargetDataSource(attrMapping), true);
                types.put(targetType.toUpperCase(Locale.ROOT), targetType);

                return new CustomComboBoxCellEditor(
                    mappingViewer,
                    mappingViewer.getTable(),
                    types.values().toArray(new String[0]),
                    SWT.BORDER);
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                return attrMapping.getTargetType(settings.getTargetDataSource(attrMapping), true);
            }

            @Override
            protected void setValue(Object element, Object value) {
                DatabaseMappingAttribute attrMapping = (DatabaseMappingAttribute) element;
                attrMapping.setTargetType(CommonUtils.toString(value));
                mappingViewer.refresh(element);
            }
        });

        columnController.addColumn(DTUIMessages.columns_mapping_dialog_column_type_text_mapping, null, SWT.LEFT, true, false, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                DatabaseMappingAttribute mapping = (DatabaseMappingAttribute) element;
                switch (mapping.getMappingType()) {
                    case existing:
                        return DTUIMessages.columns_mapping_dialog_cell_text_existing;
                    case create:
                        return DTUIMessages.columns_mapping_dialog_cell_text_new;
                    case skip:
                        return DTUIMessages.columns_mapping_dialog_cell_text_skip;
                    default:
                        return "?";
                }
            }
        });

        columnController.addColumn(
            DTUIMessages.database_consumer_page_mapping_column_transformer_text,
            DTUIMessages.database_consumer_page_mapping_column_transformer_tip,
            SWT.LEFT,
            true,
            false,
            new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    Object element = cell.getElement();
                    if (element instanceof DatabaseMappingAttribute) {
                        DataTransferAttributeTransformerDescriptor transformer = ((DatabaseMappingAttribute) element).getTransformer();
                        if (transformer != null) {
                            cell.setText(transformer.getName());
                            return;
                        }
                    }
                    cell.setText("");
                }
            });

        columnController.createColumns();

        mappingViewer.setInput(attributeMappings);

        setControl(composite);
    }

}
