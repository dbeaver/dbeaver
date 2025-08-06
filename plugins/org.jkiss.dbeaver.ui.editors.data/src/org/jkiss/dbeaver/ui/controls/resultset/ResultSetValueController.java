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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDComplexValue;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IDataController;
import org.jkiss.dbeaver.ui.data.IRowController;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.managers.DefaultValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ResultSetValueController
 */
public class ResultSetValueController implements IAttributeController, IRowController {

    private static final Log log = Log.getLog(ResultSetValueController.class);
    private static final boolean READ_ARRAY_LEAF_ALWAYS = false;

    protected final IResultSetController controller;
    protected Composite inlinePlaceholder;
    protected EditType editType;
    protected ResultSetCellLocation cellLocation;

    public ResultSetValueController(
        @NotNull IResultSetController controller,
        @NotNull ResultSetCellLocation cellLocation,
        @NotNull EditType editType,
        @Nullable Composite inlinePlaceholder) {
        this.controller = controller;
        this.cellLocation = cellLocation;
        this.editType = editType;
        this.inlinePlaceholder = inlinePlaceholder;
    }

    public void setCellLocation(ResultSetCellLocation cellLocation) {
        this.cellLocation = cellLocation;
    }

    public ResultSetRow getCurRow() {
        return cellLocation.getRow();
    }

    public void setCurRow(ResultSetRow curRow, int[] rowIndexes) {
        this.cellLocation = new ResultSetCellLocation(
            cellLocation.getAttribute(),
            curRow,
            rowIndexes);
    }

    public int[] getRowIndexes() {
        return cellLocation.getRowIndexes();
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return controller.getExecutionContext();
    }

    @NotNull
    @Override
    public IDataController getDataController() {
        return controller;
    }

    @Override
    public String getValueName() {
        return getBinding().getName();
    }

    @Override
    public DBSTypedObject getValueType() {
        DBSAttributeBase valueType = getBinding().getPresentationAttribute();
        if (cellLocation.getRowIndexes() != null) {
            if (valueType != null && valueType.getDataKind() == DBPDataKind.ARRAY) {
                DBSDataType componentType = getComponentType(valueType);
                if (componentType != null) {
                    return componentType;
                }
            }
        }
        return valueType;
    }

    @Override
    public DBDValueHandler getValueHandler() {
        DBDValueHandler valueHandler = getBinding().getValueHandler();
        if (cellLocation.getRowIndexes() != null) {
            DBSTypedObject valueType = getValueType();
            DBPDataSource dataSource = getDataController().getDataContainer().getDataSource();
            return DBUtils.findValueHandler(dataSource, valueType);
        }
        return valueHandler;
    }

    @Nullable
    private static DBSDataType getComponentType(DBSTypedObject valueType) {
        DBSDataType dataType = DBUtils.getDataType(valueType);
        if (dataType != null) {
            try {
                DBSDataType componentType = dataType.getComponentType(new VoidProgressMonitor());
                if (componentType != null) {
                    return componentType;
                }
            } catch (DBException e) {
                log.debug(e);
            }
        }
        return null;
    }

    @NotNull
    @Override
    public IRowController getRowController() {
        return this;
    }

    @NotNull
    @Override
    public DBDAttributeBinding getBinding() {
        return cellLocation.getAttribute();
    }

    public void setBinding(DBDAttributeBinding binding) {
        this.cellLocation = new ResultSetCellLocation(
            binding,
            this.cellLocation.getRow(),
            this.cellLocation.getRowIndexes());
    }

    @NotNull
    @Override
    public String getColumnId() {
        DBCExecutionContext context = getExecutionContext();
        DBSAttributeBase metaAttribute = getBinding().getMetaAttribute();
        if (metaAttribute == null) {
            metaAttribute = getBinding().getAttribute();
        }
        if (metaAttribute == null) {
            return getBinding().getName();
        }
        return DBUtils.getSimpleQualifiedName(
            context == null ? null : context.getDataSource().getContainer().getName(),
            metaAttribute instanceof DBCAttributeMetaData ? ((DBCAttributeMetaData) metaAttribute).getEntityName() : "",
            metaAttribute.getName());
    }

    @Override
    public Object getValue() {
        if (READ_ARRAY_LEAF_ALWAYS) {
            DBSTypedObject valueType = getValueType();
            boolean readLeafValue = valueType.getDataKind() != DBPDataKind.ARRAY;
            return controller.getModel().getCellValue(
                cellLocation.getAttribute(),
                cellLocation.getRow(),
                cellLocation.getRowIndexes(),
                readLeafValue);
        } else {
            return controller.getModel().getCellValue(cellLocation);
        }
    }

    @Override
    public void updateValue(@Nullable Object value, boolean updatePresentation) {
        boolean updated;
        try {
            updated = controller.updateCellValue(
                cellLocation.getAttribute(), cellLocation.getRow(), cellLocation.getRowIndexes(), value, updatePresentation);
        } catch (Exception e) {
            UIUtils.asyncExec(() -> {
                DBWorkbench.getPlatformUI().showError("Value update", "Error updating value: " + e.getMessage(), e);
            });
            return;
        }
        if (updated && updatePresentation) {
            // Update controls
            UIUtils.syncExec(() -> controller.updatePanelsContent(false));
            if (controller instanceof ResultSetViewer rsv) {
                rsv.fireResultSetChange();
            }
        }
    }

    @Override
    public void updateSelectionValue(Object value) {
        updateValue(value, true);
    }

    @Nullable
    @Override
    public DBDRowIdentifier getRowIdentifier() {
        return getBinding().getRowIdentifier();
    }

    @Override
    public IValueManager getValueManager() {
        DBSTypedObject valueType = getValueType();
        if (valueType == null) {
            return DefaultValueManager.INSTANCE;
        }

        Object value = getValue();
//        // Workaround for dynamic metadata
//        // Value type may refer to leaf attribute (e.g. String) while value is an array
//        if (value instanceof DBDCollection && valueType.getDataKind() != DBPDataKind.ARRAY) {
//            return new ArrayValueManager();
//        } else if (value instanceof DBDComposite && valueType.getDataKind() != DBPDataKind.STRUCT) {
//            return new StructValueManager();
//        }

        DBDValueHandler valueHandler = getValueHandler();

        if (cellLocation.getRowIndexes() != null) {
            try {
                // Seems to be an array item
                DBRProgressMonitor monitor = new VoidProgressMonitor();
                DBSDataType dataType = DBUtils.getDataType(valueType);
                if (dataType == null) {
                    dataType = DBUtils.resolveDataType(monitor, getBinding().getDataSource(), getBinding().getFullTypeName());
                }
                if (valueType.getDataKind() == DBPDataKind.ARRAY) {
                    DBSDataType componentType;
                    if (dataType == null) {
                        // Data type is unknown. Component type is unknown. Guess from actual value
                        DBPDataKind valueKind = DBPDataKind.STRING;
                        if (value instanceof Number) {
                            valueKind = DBPDataKind.NUMERIC;
                        } else if (value instanceof Collection<?>) {
                            valueKind = DBPDataKind.ARRAY;
                        } else if (value instanceof Map<?, ?>) {
                            valueKind = DBPDataKind.STRUCT;
                        }

                        String stringType = DBUtils.getDefaultDataTypeName(getBinding().getDataSource(), valueKind);
                        componentType = DBUtils.getLocalDataType(getBinding().getDataSource(), stringType);
                    } else {
                        componentType = dataType.getComponentType(monitor);
                    }
                    if (componentType != null) {
                        valueType = componentType;
                        valueHandler = DBUtils.findValueHandler(getBinding().getDataSource(), valueType);
                    }
                } else if (valueType.getDataKind() == DBPDataKind.STRUCT && dataType instanceof DBSEntity) {
                    final var attributes = ((DBSEntity) dataType).getAttributes(monitor);
                    final int index = cellLocation.getRowIndexes()[0];
                    if (attributes != null && attributes.size() > index) {
                        valueType = attributes.get(index);
                        valueHandler = DBUtils.findValueHandler(getBinding().getDataSource(), valueType);
                    }
                } else {
                    /*
                    // Array item value handler. Data type not recognized. Use String
                    if (getBinding().getDataSource() instanceof DBPDataTypeProvider dtp) {
                        for (DBSDataType dt : dtp.getLocalDataTypes()) {
                            if (dt.getDataKind() == DBPDataKind.STRING) {
                                valueType = dt;
                                valueHandler = DBUtils.findValueHandler(getBinding().getDataSource(), valueType);
                                break;
                            }
                        }
                    }*/
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Data type resolve", "Error resolving component data type", e);
            }
        }

        final DBCExecutionContext executionContext = getExecutionContext();
        Class<?> valueObjectType = valueHandler.getValueObjectType(valueType);
        if (valueObjectType == Object.class) {
            // Try to get type from value itself
            if (value != null) {
                valueObjectType = value.getClass();
            }
        }

        return ValueManagerRegistry.findValueManager(
            executionContext == null ? null : executionContext.getDataSource(),
            valueType,
            valueObjectType);
    }

    @Override
    public EditType getEditType() {
        return editType;
    }

    public void setEditType(EditType editType) {
        this.editType = editType;
    }

    @Override
    public boolean isReadOnly() {
        Object value = getValue();
        if (value instanceof DBDComplexValue) {
            // Complex values with non complex data kind are usually for arrays preview
            // in string format. This is read-only
            DBPDataKind dataKind = getValueType().getDataKind();
            return dataKind != DBPDataKind.ARRAY && dataKind != DBPDataKind.STRUCT;
        }
        return controller.getAttributeReadOnlyStatus(getBinding(), true, false) != null;
    }

    @Override
    public IWorkbenchPartSite getValueSite() {
        return controller.getSite();
    }

    @Nullable
    @Override
    public Composite getEditPlaceholder() {
        return inlinePlaceholder;
    }

    @Override
    public void refreshEditor() {
        controller.updatePanelsContent(true);
    }

    @Override
    public void showMessage(String message, DBPMessageType messageType) {
        UIUtils.asyncExec(() -> controller.setStatus(message, messageType));
    }

    @NotNull
    @Override
    public List<DBDAttributeBinding> getRowAttributes() {
        return Arrays.asList(controller.getModel().getAttributes());
    }

    @Nullable
    @Override
    public Object getAttributeValue(DBDAttributeBinding attribute) {
        return controller.getModel().getCellValue(attribute, cellLocation.getRow());
    }

}
