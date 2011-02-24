/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
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

    protected DBDContent getColumnValue(DBCExecutionContext context, ResultSet resultSet, DBSTypedObject column,
                                        int columnIndex)
        throws DBCException, SQLException
    {
        Object value;
        if (JDBCUtils.isDriverODBC(context)) {
            switch (column.getValueType()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    value = resultSet.getString(columnIndex);
                    break;
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                case java.sql.Types.BLOB:
                    value = resultSet.getBytes(columnIndex);
                    break;
                default:
                    value = resultSet.getObject(columnIndex);
                    break;
            }
        } else {
            value = resultSet.getObject(columnIndex);
        }
        if (value == null) {
            // Create wrapper using column type
            switch (column.getValueType()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                    return new JDBCContentChars(null);
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return new JDBCContentCLOB(null);
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(null);
                case java.sql.Types.BLOB:
                    return new JDBCContentBLOB(null);
                default:
                    throw new DBCException("Unsupported column type: " + column.getTypeName());
            }
        } else if (value instanceof byte[]) {
            return new JDBCContentBytes((byte[]) value);
        } else if (value instanceof String) {
            return new JDBCContentChars((String) value);
        } else if (value instanceof Blob) {
            return new JDBCContentBLOB((Blob) value);
        } else if (value instanceof Clob) {
            return new JDBCContentCLOB((Clob) value);
        } else {
            throw new DBCException("Unsupported value type: " + value.getClass().getName());
        }
    }

    protected void bindParameter(
        DBCExecutionContext context,
        PreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(context, statement, paramType, paramIndex);
        } else {
            throw new DBCException("Unsupported value type: " + value);
        }
    }

    public Class getValueObjectType()
    {
        return DBDContent.class;
    }

    public Object copyValueObject(DBCExecutionContext context, Object value)
        throws DBCException
    {
        if (value instanceof DBDValueClonable) {
            return ((DBDValueClonable)value).cloneValue(context.getProgressMonitor());
        }
        // Copy not supported
        if (value instanceof DBDValue) {
            return ((DBDValue)value).makeNull();
        }
        return null;
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).release();
        }
        super.releaseValueObject(value);    //To change body of overridden methods use File | Settings | File Templates.
    }

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

    public void fillContextMenu(IMenuManager menuManager, final DBDValueController controller)
        throws DBCException
    {
        if (controller.getValue() instanceof DBDContent && !((DBDContent)controller.getValue()).isNull()) {
            menuManager.add(new Action("Save to file ...", DBIcon.SAVE.getImageDescriptor()) {
                @Override
                public void run() {
                    saveToFile(controller);
                }
            });
        }
        menuManager.add(new Action("Load from file ...", DBIcon.LOAD.getImageDescriptor()) {
            @Override
            public void run() {
                loadFromFile(controller);
            }
        });
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                    "content_type",
                    "Content Type",
                    ((DBDContent)value).getContentType());
                propertySource.addProperty(
                    "content_length",
                    "Content Length",
                    ((DBDContent)value).getContentLength());
            }
        }
        catch (Exception e) {
            log.warn("Could not extract LOB value information", e);
        }
        propertySource.addProperty(
            "max_length",
            "Max Length",
            controller.getColumnMetaData().getDisplaySize());
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            // Open inline editor
            if (controller.getValue() instanceof JDBCContentChars) {
                // String editor
                JDBCContentChars value = (JDBCContentChars)controller.getValue();

                Text editor = new Text(controller.getInlinePlaceholder(), SWT.NONE);
                editor.setText(value.getData() == null ? "" : value.getData());
                editor.setEditable(!controller.isReadOnly());
                int maxLength = controller.getColumnMetaData().getDisplaySize();
                if (maxLength <= 0) {
                    maxLength = MAX_STRING_LENGTH;
                } else {
                    maxLength = Math.min(maxLength, MAX_STRING_LENGTH);
                }
                editor.setTextLimit(maxLength);
                editor.selectAll();
                editor.setFocus();
                initInlineControl(controller, editor, new ValueExtractor<Text>() {
                    public Object getValueFromControl(Text control)
                    {
                        String newValue = control.getText();
                        return new JDBCContentChars(newValue);
                    }
                });
                return true;
            } else {
                controller.showMessage("LOB and binary data can't be edited inline", true);
                return false;
            }
        }
        // Open LOB editor
        Object value = controller.getValue();
        if (value instanceof DBDContent) {
            boolean isText = ContentUtils.isTextContent((DBDContent)value);
            if (isText) {
                // Check for length
            }
            List<IContentEditorPart> parts = new ArrayList<IContentEditorPart>();
            if (isText) {
                parts.add(new ContentTextEditorPart());
            } else {
                parts.add(new ContentBinaryEditorPart());
                parts.add(new ContentTextEditorPart());
                parts.add(new ContentImageEditorPart());
            }
            return ContentEditor.openEditor(
                controller,
                parts.toArray(new IContentEditorPart[parts.size()]) );
        } else {
            controller.showMessage("Unsupported content value type", true);
            return false;
        }
    }

    private void loadFromFile(final DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error("Bad content value: " + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File openFile = ContentUtils.openFile(shell);
        if (openFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage;
                        if (ContentUtils.isTextContent(value)) {
                            storage = new ExternalContentStorage(openFile, ContentUtils.DEFAULT_FILE_CHARSET);
                        } else {
                            storage = new ExternalContentStorage(openFile);
                        }
                        value.updateContents(monitor, storage);
                        controller.updateValue(value);
                    }
                    catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                "Could not load content",
                "Could not load content from file '" + openFile.getAbsolutePath() + "'",
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    private void saveToFile(DBDValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error("Bad content value: " + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File saveFile = ContentUtils.selectFileForSave(shell);
        if (saveFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage = value.getContents(monitor);
                        if (ContentUtils.isTextContent(value)) {
                            ContentUtils.saveContentToFile(
                                storage.getContentReader(),
                                saveFile,
                                ContentUtils.DEFAULT_FILE_CHARSET,
                                monitor
                            );
                        } else {
                            ContentUtils.saveContentToFile(
                                storage.getContentStream(),
                                saveFile,
                                monitor
                            );
                        }
                    }
                    catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                "Could not save content",
                "Could not save content to file '" + saveFile.getAbsolutePath() + "'",
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

}
