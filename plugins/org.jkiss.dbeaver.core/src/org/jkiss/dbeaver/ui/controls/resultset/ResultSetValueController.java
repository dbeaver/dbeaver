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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
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
import org.jkiss.dbeaver.ui.data.registry.DataManagerRegistry;

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
    protected final DBDAttributeBinding binding;

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
    public void updateValue(@Nullable Object value)
    {
        if (controller.getModel().updateCellValue(binding, curRow, value)) {
            // Update controls
            controller.getSite().getShell().getDisplay().syncExec(new Runnable() {
                @Override
                public void run() {
                    controller.updatePanelsContent();
                }
            });
        }
        controller.fireResultSetChange();
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
        return DataManagerRegistry.findValueManager(
            getDataSourceContainer(),
            valueType,
            getValueHandler().getValueObjectType(valueType));
    }

    @Override
    public EditType getEditType()
    {
        return editType;
    }

    @Override
    public boolean isReadOnly()
    {
        return controller.isAttributeReadOnly(binding);
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

    @Nullable
    @Override
    public IContributionManager getEditBar()
    {
        return null;
    }

    @Override
    public void showMessage(String message, boolean error)
    {
        controller.setStatus(message, error);
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
