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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentXMLEditorPart;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 *
 * @author Serge Rider
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCContentValueHandler.class);

    public static final JDBCContentValueHandler INSTANCE = new JDBCContentValueHandler();

    private static final int MAX_STRING_LENGTH = 0xfffff;

    @Override
    protected DBDContent fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        if (value == null && !resultSet.wasNull()) {
            // This may happen in some bad drivers like ODBC bridge
            switch (type.getTypeID()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    value = resultSet.getString(index);
                    break;
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                case java.sql.Types.BLOB:
                    value = resultSet.getBytes(index);
                    break;
                case java.sql.Types.SQLXML:
                    value = resultSet.getSQLXML(index);
                    break;
                default:
                    value = resultSet.getObject(index);
                    break;
            }
        }
        return getValueFromObject(context, type, value, false);
    }

    @Override
    protected void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(context, statement, paramType, paramIndex);
        } else {
            throw new DBCException(CoreMessages.model_jdbc_unsupported_value_type_ + value);
        }
    }

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_INLINE_EDITOR | FEATURE_SHOW_ICON;
    }

    @Override
    public Class getValueObjectType()
    {
        return DBDContent.class;
    }

    @Override
    public DBDContent getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            // Create wrapper using column type
            switch (type.getTypeID()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                    return new JDBCContentChars(context.getDataSource(), null);
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return new JDBCContentCLOB(context.getDataSource(), null);
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(context.getDataSource(), null);
                case java.sql.Types.BLOB:
                    return new JDBCContentBLOB(context.getDataSource(), null);
                case java.sql.Types.SQLXML:
                    return new JDBCContentXML(context.getDataSource(), null);
                default:
                    throw new DBCException(CoreMessages.model_jdbc_unsupported_column_type_ + type.getTypeName());
            }
        } else if (object instanceof byte[]) {
            return new JDBCContentBytes(context.getDataSource(), (byte[]) object);
        } else if (object instanceof String) {
            return new JDBCContentChars(context.getDataSource(), (String) object);
        } else if (object instanceof Blob) {
            return new JDBCContentBLOB(context.getDataSource(), (Blob) object);
        } else if (object instanceof Clob) {
            return new JDBCContentCLOB(context.getDataSource(), (Clob) object);
        } else if (object instanceof SQLXML) {
            return new JDBCContentXML(context.getDataSource(), (SQLXML) object);
        } else if (object instanceof DBDContent && object instanceof DBDValueCloneable) {
            return (DBDContent) ((DBDValueCloneable)object).cloneValue(context.getProgressMonitor());
        } else {
            throw new DBCException(CoreMessages.model_jdbc_unsupported_value_type_ + object.getClass().getName());
        }
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, Object value)
    {
        if (value instanceof DBDContent) {
            String result = value.toString();
            if (result == null) {
                return super.getValueDisplayString(column, null);
            } else {
                return result;
            }
        }
        return super.getValueDisplayString(column, value);
    }

    @Override
    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
        if (controller.getValue() instanceof DBDContent && !((DBDContent)controller.getValue()).isNull()) {
            menuManager.add(new Action(CoreMessages.model_jdbc_save_to_file_, DBIcon.SAVE.getImageDescriptor()) {
                @Override
                public void run() {
                    saveToFile(controller);
                }
            });
        }
        menuManager.add(new Action(CoreMessages.model_jdbc_load_from_file_, DBIcon.LOAD.getImageDescriptor()) {
            @Override
            public void run() {
                loadFromFile(controller);
            }
        });
    }

    @Override
    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                    "content_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_content_type,
                    ((DBDContent)value).getContentType());
                final long contentLength = ((DBDContent) value).getContentLength();
                if (contentLength >= 0) {
                    propertySource.addProperty(
                        "content_length", //$NON-NLS-1$
                        CoreMessages.model_jdbc_content_length,
                        contentLength);
                }
            }
        }
        catch (Exception e) {
            log.warn("Could not extract LOB value information", e); //$NON-NLS-1$
        }
        propertySource.addProperty(
            "max_length", //$NON-NLS-1$
            CoreMessages.model_jdbc_max_length,
            controller.getAttributeMetaData().getMaxLength());
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                // Open inline/panel editor
                if (controller.getValue() instanceof JDBCContentChars) {
                    // String editor
                    return new ValueEditor<Text>(controller) {
                        @Override
                        public void refreshValue()
                        {
                            JDBCContentChars newValue = (JDBCContentChars) valueController.getValue();
                            control.setText(newValue.getData() == null ? "" : newValue.getData()); //$NON-NLS-1$
                        }
                        @Override
                        protected Text createControl(Composite editPlaceholder)
                        {
                            final Text editor = new Text(editPlaceholder, SWT.NONE);
                            editor.setEditable(!valueController.isReadOnly());
                            long maxLength = valueController.getAttributeMetaData().getMaxLength();
                            if (maxLength <= 0) {
                                maxLength = MAX_STRING_LENGTH;
                            } else {
                                maxLength = Math.min(maxLength, MAX_STRING_LENGTH);
                            }
                            editor.setTextLimit((int) maxLength);
                            editor.selectAll();
                            return editor;
                        }
                        @Override
                        public Object extractValue(DBRProgressMonitor monitor)
                        {
                            String newValue = control.getText();
                            return new JDBCContentChars(valueController.getDataSource(), newValue);
                        }
                    };
                } else {
                    return null;
                }
            }
            case EDITOR:
            {
                // Open LOB editor
                Object value = controller.getValue();
                if (value instanceof DBDContent && controller instanceof DBDAttributeController) {
                    DBDContent content = (DBDContent)value;
                    boolean isText = ContentUtils.isTextContent(content);
                    List<IContentEditorPart> parts = new ArrayList<IContentEditorPart>();
                    if (isText) {
                        parts.add(new ContentTextEditorPart());
                        if (isXML(content)) {
                            parts.add(new ContentXMLEditorPart());
                        }
                    } else {
                        parts.add(new ContentBinaryEditorPart());
                        parts.add(new ContentTextEditorPart());
                        parts.add(new ContentImageEditorPart());
                    }
                    return ContentEditor.openEditor(
                        (DBDAttributeController)controller,
                        parts.toArray(new IContentEditorPart[parts.size()]) );
                } else {
                    controller.showMessage(CoreMessages.model_jdbc_unsupported_content_value_type_, true);
                    return null;
                }
            }
            case PANEL:
            {
                final DBDContent content = (DBDContent) controller.getValue();
                final IContentEditorPart editor;
                if (ContentUtils.isTextContent(content)) {
                    if (isXML(content)) {
                        editor = new ContentXMLEditorPart();
                    } else {
                        editor = new ContentTextEditorPart();
                    }
                } else {
                    editor = new ContentBinaryEditorPart();
                }
                final ContentEditorInput input;
                try {
                    input = new ContentEditorInput((DBDAttributeController) controller, new IContentEditorPart[]{editor}, VoidProgressMonitor.INSTANCE);
                    editor.init(new SubEditorSite(controller.getValueSite()),
                        input);
                } catch (PartInitException e) {
                    log.error("Can't initialize content editor", e);
                    return null;
                }
                return new ValueEditorEx<Control>(controller) {
                    private DBDContent prevValue = content;
                    @Override
                    public void showValueEditor()
                    {
                    }

                    @Override
                    public void closeValueEditor()
                    {
                        editor.dispose();
                    }

                    @Override
                    public void refreshValue()
                    {
                        if (prevValue == controller.getValue()) {
                            // No need to refresh
                            return;
                        }
                        prevValue = (DBDContent) controller.getValue();
                        try {
                            input.refreshContent(VoidProgressMonitor.INSTANCE, (DBDAttributeController) controller);
                        } catch (DBException e) {
                            log.error("Error refreshing content value", e);
                        }
                    }

                    @Override
                    public Object extractValue(DBRProgressMonitor monitor) throws DBException
                    {
                        editor.doSave(monitor.getNestedMonitor());
                        input.updateContentFromFile(monitor.getNestedMonitor());
                        return input.getContent();
                    }

                    @Override
                    protected Control createControl(Composite editPlaceholder)
                    {
                        editor.createPartControl(controller.getEditPlaceholder());
                        return editor.getEditorControl();
                    }
                };
            }
            default:
                return null;
        }
    }

    private static boolean isXML(DBDContent content)
    {
        return MimeTypes.TEXT_XML.equalsIgnoreCase(content.getContentType());
    }

    private void loadFromFile(final DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File openFile = ContentUtils.openFile(shell);
        if (openFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage;
                        if (ContentUtils.isTextContent(value)) {
                            storage = new ExternalContentStorage(openFile, ContentUtils.DEFAULT_FILE_CHARSET_NAME);
                        } else {
                            storage = new ExternalContentStorage(openFile);
                        }
                        value.updateContents(monitor, storage);
                        controller.updateValue(value);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                CoreMessages.model_jdbc_could_not_load_content,
                CoreMessages.model_jdbc_could_not_load_content_from_file + openFile.getAbsolutePath() + "'", //$NON-NLS-2$
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    private void saveToFile(DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File saveFile = ContentUtils.selectFileForSave(shell);
        if (saveFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage = value.getContents(monitor);
                        if (ContentUtils.isTextContent(value)) {
                            ContentUtils.saveContentToFile(
                                storage.getContentReader(),
                                saveFile,
                                ContentUtils.DEFAULT_FILE_CHARSET_NAME,
                                monitor
                            );
                        } else {
                            ContentUtils.saveContentToFile(
                                storage.getContentStream(),
                                saveFile,
                                monitor
                            );
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                CoreMessages.model_jdbc_could_not_save_content,
                CoreMessages.model_jdbc_could_not_save_content_to_file_ + saveFile.getAbsolutePath() + "'", //$NON-NLS-2$
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

}
