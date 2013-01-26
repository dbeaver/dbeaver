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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);
    private static final String CELL_VALUE_INLINE_EDITOR = "org.jkiss.dbeaver.CellValueInlineEditor";

    @Override
    public final Object fetchValueObject(DBCExecutionContext context, DBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException
    {
        try {
            return fetchColumnValue(context, (JDBCResultSet) resultSet, type, index + 1);
        }
        catch (Throwable e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_get_result_set_value, e);
        }
    }

    @Override
    public final void bindValueObject(DBCExecutionContext context, DBCStatement statement, DBSTypedObject columnMetaData,
                                      int index, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCExecutionContext) context, (JDBCPreparedStatement) statement, columnMetaData, index + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Override
    public Object getValueFromClipboard(DBCExecutionContext context, DBSTypedObject column, Clipboard clipboard) throws DBCException
    {
        String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
        if (CommonUtils.isEmpty(strValue)) {
            return null;
        }
        return getValueFromObject(context, column, strValue, false);
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format) {
        return value == null ? DBConstants.NULL_VALUE_LABEL : value.toString();
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {

    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "column_size", //$NON-NLS-1$
            CoreMessages.model_jdbc_column_size,
            controller.getAttributeMetaData().getMaxLength());
    }

    protected abstract Object fetchColumnValue(DBCExecutionContext context, JDBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException, SQLException;

    protected abstract void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

    protected abstract class ValueEditor<T extends Control> implements DBDValueEditor {
        protected final DBDValueController valueController;
        protected final T control;
        private boolean activated;
        protected ValueEditor(final DBDValueController valueController)
        {
            this.valueController = valueController;
            this.control = createControl(valueController.getEditPlaceholder());
            if (this.control != null) {
                initInlineControl(this.control);
            }
            ToolBar editToolBar = valueController.getEditToolBar();
            if (editToolBar != null) {
                if (!valueController.isReadOnly()) {
                    UIUtils.createToolItem(editToolBar, "Save changes", DBIcon.SAVE.getImage(), new Action("Save") {
                        @Override
                        public void run()
                        {
                            saveValue();
                        }
                    });
                }
            }
        }

        @Override
        public Control getControl()
        {
            return control;
        }

        public abstract Object extractValue(DBRProgressMonitor monitor) throws DBException;

        protected abstract T createControl(Composite editPlaceholder);

        protected void initInlineControl(final Control inlineControl)
        {
            boolean isInline = (valueController.getEditType() == DBDValueController.EditType.INLINE);

            // Panel controls
            inlineControl.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e)
                {
                    if (!activated) {
                        UIUtils.enableHostEditorKeyBindings(valueController.getValueSite(), false);
                        activated = true;
                    }
                }
                @Override
                public void focusLost(FocusEvent e)
                {
                    if (activated) {
                        UIUtils.enableHostEditorKeyBindings(valueController.getValueSite(), true);
                        activated = false;
                    }
                }
            });
            inlineControl.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    if (activated) {
                        UIUtils.enableHostEditorKeyBindings(valueController.getValueSite(), true);
                        activated = false;
                    }
                }
            });


//            if (!isInline) {
//                inlineControl.setBackground(valueController.getEditPlaceholder().getBackground());
//            }

            if (isInline) {
                inlineControl.setFont(valueController.getEditPlaceholder().getFont());
                // There is a bug in windows. First time date control gain focus it renders cell editor incorrectly.
                // Let's focus on it in async mode
                inlineControl.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        inlineControl.setFocus();
                    }
                });

                inlineControl.addTraverseListener(new TraverseListener() {
                    @Override
                    public void keyTraversed(TraverseEvent e)
                    {
                        if (e.detail == SWT.TRAVERSE_RETURN) {
                            saveValue();
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        } else if (e.detail == SWT.TRAVERSE_ESCAPE) {
                            valueController.closeInlineEditor();
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        } else if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                            saveValue();
                            valueController.nextInlineEditor(e.detail == SWT.TRAVERSE_TAB_NEXT);
                            e.doit = false;
                            e.detail = SWT.TRAVERSE_NONE;
                        }
                    }
                });
                inlineControl.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e)
                    {
                        // Check new focus control in async mode
                        // (because right now focus is still on edit control)
                        inlineControl.getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run()
                            {
                                if (inlineControl.isDisposed()) {
                                    return;
                                }
                                Control newFocus = inlineControl.getDisplay().getFocusControl();
                                if (newFocus != null) {
                                    for (Control fc = newFocus.getParent(); fc != null; fc = fc.getParent()) {
                                        if (fc == valueController.getEditPlaceholder()) {
                                            // New focus is still a child of inline placeholder - do not close it
                                            return;
                                        }
                                    }
                                }
                                saveValue();
                            }
                        });
                    }
                });
            }
        }

        private void saveValue()
        {
            DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        Object newValue = extractValue(monitor);
                        valueController.closeInlineEditor();
                        valueController.updateValue(newValue);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
    }

    protected abstract class ValueEditorEx<T extends Control> extends ValueEditor<T> implements DBDValueEditorEx {

        protected ValueEditorEx(final DBDValueController valueController)
        {
            super(valueController);
        }
    }

}