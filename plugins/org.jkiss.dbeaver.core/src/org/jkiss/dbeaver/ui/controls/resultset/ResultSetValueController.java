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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IRowController;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;

import java.util.Arrays;
import java.util.List;

/**
* ResultSetValueController
*/
public class ResultSetValueController implements IAttributeController, IRowController {

    protected final ResultSetViewer controller;
    protected final EditType editType;
    protected final Composite inlinePlaceholder;
    protected ResultSetRow curRow;
    protected DBDAttributeBinding binding;

    public ResultSetValueController(
        @NotNull IResultSetController controller,
        @NotNull DBDAttributeBinding binding,
        @NotNull ResultSetRow row,
        @NotNull EditType editType,
        @Nullable Composite inlinePlaceholder)
    {
        this.controller = (ResultSetViewer) controller;
        this.binding = binding;
        this.curRow = row;
        this.editType = editType;
        this.inlinePlaceholder = inlinePlaceholder;
    }

    public ResultSetRow getCurRow() {
        return curRow;
    }

    public void setCurRow(ResultSetRow curRow)
    {
        this.curRow = curRow;
    }


    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return controller.getExecutionContext();
    }

    @Override
    public String getValueName()
    {
        return binding.getName();
    }

    @Override
    public DBSTypedObject getValueType()
    {
        return binding.getAttribute();
    }

    @NotNull
    @Override
    public IRowController getRowController() {
        return this;
    }

    @NotNull
    @Override
    public DBDAttributeBinding getBinding()
    {
        return binding;
    }

    public void setBinding(DBDAttributeBinding binding) {
        this.binding = binding;
    }

    @NotNull
    @Override
    public String getColumnId() {
        DBCExecutionContext context = getExecutionContext();
        DBCAttributeMetaData metaAttribute = binding.getMetaAttribute();
        return DBUtils.getSimpleQualifiedName(
            context == null ? null : context.getDataSource().getContainer().getName(),
            metaAttribute.getEntityName(),
            metaAttribute.getName());
    }

    @Override
    public Object getValue()
    {
        return controller.getModel().getCellValue(binding, curRow);
    }

    @Override
    public void updateValue(@Nullable Object value, boolean updatePresentation)
    {
        boolean updated = controller.getModel().updateCellValue(binding, curRow, value);
        if (updated && updatePresentation) {
            // Update controls
            DBeaverUI.syncExec(new Runnable() {
                @Override
                public void run() {
                    controller.updatePanelsContent(false);
                }
            });
            controller.fireResultSetChange();
        }
    }

    @Nullable
    @Override
    public DBDRowIdentifier getRowIdentifier()
    {
        return binding.getRowIdentifier();
    }

    @Override
    public DBDValueHandler getValueHandler()
    {
        return binding.getValueHandler();
    }

    private DBPDataSourceContainer getDataSourceContainer() {
        final IResultSetContainer rsContainer = controller.getContainer();
        if (rsContainer instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) rsContainer).getDataSourceContainer();
        } else {
            final DBCExecutionContext executionContext = getExecutionContext();
            return executionContext == null ? null : executionContext.getDataSource().getContainer();
        }
    }

    @Override
    public IValueManager getValueManager() {
        DBSAttributeBase valueType = binding.getPresentationAttribute();
        final DBCExecutionContext executionContext = getExecutionContext();
        Class<?> valueObjectType = getValueHandler().getValueObjectType(valueType);
        if (valueObjectType == Object.class) {
            // Try to get type from value itself
            Object value = getValue();
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
    public EditType getEditType()
    {
        return editType;
    }

    @Override
    public boolean isReadOnly()
    {
        return controller.getModel().isAttributeReadOnly(binding);
    }

    @Override
    public IWorkbenchPartSite getValueSite()
    {
        return controller.getSite();
    }

    @Nullable
    @Override
    public Composite getEditPlaceholder()
    {
        return inlinePlaceholder;
    }

    @Override
    public void refreshEditor() {
        controller.updatePanelsContent(true);
    }

    @Override
    public void showMessage(String message, DBPMessageType messageType)
    {
        controller.setStatus(message, messageType);
    }

    @NotNull
    @Override
    public List<DBDAttributeBinding> getRowAttributes()
    {
        return Arrays.asList(controller.getModel().getAttributes());
    }

    @Nullable
    @Override
    public Object getAttributeValue(DBDAttributeBinding attribute)
    {
        return controller.getModel().getCellValue(attribute, curRow);
    }

}
