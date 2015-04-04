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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.data.editors.ContentInlineEditor;
import org.jkiss.dbeaver.model.impl.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 *
 * @author Serge Rider
 */
public class JDBCContentValueHandler extends JDBCAbstractValueHandler {

    static final Log log = Log.getLog(JDBCContentValueHandler.class);

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
        ContentUtils.contributeContentActions(manager, controller);
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
                // Open inline/panel editor
                if (controller.getValue() instanceof DBDContentCached) {
                    return new ContentInlineEditor(controller);
                } else {
                    return null;
                }
            case EDITOR:
                return ContentUtils.openContentEditor(controller);
            case PANEL:
                return new ContentPanelEditor(controller);
            default:
                return null;
        }
    }

}
