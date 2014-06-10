/*
 * Copyright (C) 2010-2014 Serge Rieder
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
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.SQLException;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    static final Log log = LogFactory.getLog(JDBCAbstractValueHandler.class);

    @Override
    public final Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index)
        throws DBCException
    {
        try {
            if (resultSet instanceof JDBCResultSet) {
                return fetchColumnValue(session, (JDBCResultSet) resultSet, type, index + 1);
            } else {
                return resultSet.getColumnValue(index + 1);
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
    }

    @Override
    public final void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject columnMetaData,
                                      int index, Object value) throws DBCException {
        try {
            this.bindParameter((JDBCSession) session, (JDBCPreparedStatement) statement, columnMetaData, index + 1, value);
        }
        catch (SQLException e) {
            throw new DBCException(CoreMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        return DBUtils.getDefaultValueDisplayString(value, format);
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull DBDValueController controller)
        throws DBCException
    {

    }

    @Override
    public void contributeProperties(@NotNull PropertySourceAbstract propertySource, @NotNull DBDValueController controller)
    {
    }

    @Nullable
    protected abstract Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
        throws DBCException, SQLException;

    protected abstract void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException;

    @Override
    public DBCLogicalOperator[] getSupportedOperators(@NotNull DBDAttributeBinding attribute) {
        return DBUtils.getDefaultOperators(attribute);
    }

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
                        if (!inlineControl.isDisposed()) {
                            inlineControl.setFocus();
                        }
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
            try {
                Object newValue = extractEditorValue();
                valueController.closeInlineEditor();
                valueController.updateValue(newValue);
            } catch (DBException e) {
                UIUtils.showErrorDialog(getControl().getShell(), "Value save", "Can't save edited value", e);
            }
        }
    }

    protected abstract class ValueEditorEx<T extends Control> extends ValueEditor<T> implements DBDValueEditorStandalone {

        protected ValueEditorEx(final DBDValueController valueController)
        {
            super(valueController);
        }
    }

}