/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.Arrays;
import java.util.List;

/**
* ResultSetValueController
*/
public class ResultSetValueController implements DBDAttributeController, DBDRowController {

    protected final IResultSetController controller;
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
        this.controller = controller;
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
        return binding.getMetaAttribute();
    }

    @NotNull
    @Override
    public DBDRowController getRowController() {
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
                    controller.updateValueView();
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
    public void closeInlineEditor()
    {
    }

    @Override
    public void nextInlineEditor(boolean next) {
    }

    @Override
    public void unregisterEditor(DBDValueEditorStandalone editor) {
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
