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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.impl.data.editors.ContentInlineEditor;
import org.jkiss.dbeaver.model.impl.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentXMLEditorPart;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.*;
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

    public static final String PROP_CATEGORY_CONTENT = "LOB";

    @Override
    protected DBDContent fetchColumnValue(
        DBCSession session,
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
        return getValueFromObject(session, type, value, false);
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(session, statement, paramType, paramIndex);
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
    public DBDContent getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            // Create wrapper using column type
            switch (type.getTypeID()) {
                case java.sql.Types.CHAR:
                case java.sql.Types.VARCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.LONGVARCHAR:
                case java.sql.Types.LONGNVARCHAR:
                    return new JDBCContentChars(session.getDataSource(), null);
                case java.sql.Types.CLOB:
                case java.sql.Types.NCLOB:
                    return new JDBCContentCLOB(session.getDataSource(), null);
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(session.getDataSource());
                case java.sql.Types.BLOB:
                    return new JDBCContentBLOB(session.getDataSource(), null);
                case java.sql.Types.SQLXML:
                    return new JDBCContentXML(session.getDataSource(), null);
                default:
                    log.error(CoreMessages.model_jdbc_unsupported_column_type_ + type.getTypeName());
                    return new JDBCContentBytes(session.getDataSource());
            }
        } else if (object instanceof byte[]) {
            return new JDBCContentBytes(session.getDataSource(), (byte[]) object);
        } else if (object instanceof String) {
            // String is a default format in many cases (like clipboard transfer)
            // So it is possible that real object type isn't string
            switch (type.getTypeID()) {
                case java.sql.Types.BINARY:
                case java.sql.Types.VARBINARY:
                case java.sql.Types.LONGVARBINARY:
                    return new JDBCContentBytes(session.getDataSource(), (String) object);
                default:
                    // String by default
                    return new JDBCContentChars(session.getDataSource(), (String) object);
            }
        } else if (object instanceof Blob) {
            return new JDBCContentBLOB(session.getDataSource(), (Blob) object);
        } else if (object instanceof Clob) {
            return new JDBCContentCLOB(session.getDataSource(), (Clob) object);
        } else if (object instanceof SQLXML) {
            return new JDBCContentXML(session.getDataSource(), (SQLXML) object);
        } else if (object instanceof DBDContent && object instanceof DBDValueCloneable) {
            return (DBDContent) ((DBDValueCloneable)object).cloneValue(session.getProgressMonitor());
        } else {
            throw new DBCException(CoreMessages.model_jdbc_unsupported_value_type_ + object.getClass().getName());
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof DBDContent) {
            String result = ((DBDContent) value).getDisplayString(format);
            if (result == null) {
                return super.getValueDisplayString(column, null, format);
            } else {
                return result;
            }
        }
        return super.getValueDisplayString(column, value, format);
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final DBDValueController controller)
        throws DBCException
    {
        if (controller.getValue() instanceof DBDContent && !((DBDContent)controller.getValue()).isNull()) {
            manager.add(new Action(CoreMessages.model_jdbc_save_to_file_, DBIcon.SAVE.getImageDescriptor()) {
                @Override
                public void run() {
                    saveToFile(controller);
                }
            });
        }
        manager.add(new Action(CoreMessages.model_jdbc_load_from_file_, DBIcon.LOAD.getImageDescriptor()) {
            @Override
            public void run() {
                loadFromFile(controller);
            }
        });
    }

    @Override
    public void contributeProperties(@NotNull PropertySourceAbstract propertySource, @NotNull DBDValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                    PROP_CATEGORY_CONTENT,
                    "content_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_content_type,
                    ((DBDContent)value).getContentType());
                final long contentLength = ((DBDContent) value).getContentLength();
                if (contentLength >= 0) {
                    propertySource.addProperty(
                        PROP_CATEGORY_CONTENT,
                        "content_length", //$NON-NLS-1$
                        CoreMessages.model_jdbc_content_length,
                        contentLength);
                }
            }
        }
        catch (Exception e) {
            log.warn("Could not extract LOB value information", e); //$NON-NLS-1$
        }
    }

    @Override
    public DBDValueEditor createEditor(@NotNull final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                // Open inline/panel editor
                if (controller.getValue() instanceof DBDContentCached) {
                    final boolean isText = ContentUtils.isTextContent(((DBDContent) controller.getValue()));
                    // String editor
                    return new ContentInlineEditor(controller, isText);
                } else {
                    return null;
                }
            }
            case EDITOR:
            {
                // Open LOB editor
                Object value = controller.getValue();
                DBDValueController.EditType binaryEditType = DBDValueController.EditType.valueOf(
                    controller.getDataSource().getContainer().getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
                if (binaryEditType != DBDValueController.EditType.EDITOR && value instanceof DBDContentCached) {
                    // Use string editor for cached content
                    return new TextViewDialog(controller);
                } else if (value instanceof DBDContent) {
                    DBDContent content = (DBDContent)value;
                    boolean isText = ContentUtils.isTextContent(content);
                    List<ContentEditorPart> parts = new ArrayList<ContentEditorPart>();
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
                        controller,
                        parts.toArray(new ContentEditorPart[parts.size()]) );
                } else {
                    controller.showMessage(CoreMessages.model_jdbc_unsupported_content_value_type_, true);
                    return null;
                }
            }
            case PANEL:
            {
                return new ContentPanelEditor(controller);
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
        DBeaverUI.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
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
