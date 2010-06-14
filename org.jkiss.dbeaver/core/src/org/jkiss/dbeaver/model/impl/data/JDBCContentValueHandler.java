/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentBinaryEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentImageEditorPart;
import org.jkiss.dbeaver.ui.editors.content.parts.ContentTextEditorPart;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler {

    static Log log = LogFactory.getLog(JDBCContentValueHandler.class);

    public static final JDBCContentValueHandler INSTANCE = new JDBCContentValueHandler();

    private static final int MAX_STRING_LENGTH = 0xfffff;

    protected DBDContent getColumnValue(ResultSet resultSet, DBSTypedObject columnType, int columnIndex)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(columnIndex);
        if (value == null) {
            // Create wrapper using column type
            switch (columnType.getValueType()) {
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
                    throw new DBCException("Unsupported column type: " + columnType.getTypeName());
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

    protected void bindParameter(PreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws DBCException, SQLException
    {
        if (value instanceof JDBCContentAbstract) {
            ((JDBCContentAbstract)value).bindParameter(statement, paramType, paramIndex);
        } else {
            throw new DBCException("Unsupported value type: " + value);
        }
    }

    @Override
    public void releaseValueObject(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).release();
        }
        super.releaseValueObject(value);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getValueDisplayString(Object value)
    {
        if (value instanceof DBDContent) {
            String result = value.toString();
            if (result != null) {
                return result;
            } else {
                return super.getValueDisplayString(null);
            }
        }
        return super.getValueDisplayString(value);
    }

    public void fillContextMenu(IMenuManager menuManager, DBDValueController controller)
        throws DBCException
    {
        Action saveAction = new Action() {
            @Override
            public void run() {
            }
        };
        saveAction.setText("Save to file ...");
        menuManager.add(saveAction);
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
        boolean isText = value instanceof DBDContentCharacter;
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
    }

}
