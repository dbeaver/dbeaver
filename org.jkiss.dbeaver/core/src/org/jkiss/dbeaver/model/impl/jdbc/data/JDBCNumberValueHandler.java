/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSColumnBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.data.NumberViewDialog;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceAbstract;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCNumberValueHandler INSTANCE = new JDBCNumberValueHandler();

    static final Log log = LogFactory.getLog(JDBCNumberValueHandler.class);

    private static final int MAX_NUMBER_LENGTH = 100;

    protected Object getColumnValue(DBRProgressMonitor monitor, ResultSet resultSet, DBSColumnBase column,
                                    int columnIndex)
        throws DBCException, SQLException
    {
        switch (column.getValueType()) {
        case java.sql.Types.BIGINT:
            return resultSet.getLong(columnIndex);
        case java.sql.Types.FLOAT:
            return resultSet.getFloat(columnIndex);
        case java.sql.Types.INTEGER:
            return resultSet.getInt(columnIndex);
        case java.sql.Types.SMALLINT:
            return resultSet.getShort(columnIndex);
        case java.sql.Types.TINYINT:
        case java.sql.Types.BIT:
            return resultSet.getByte(columnIndex);
        default:
            if (column.getScale() > 0) {
                return resultSet.getDouble(columnIndex);
            } else {
                return resultSet.getLong(columnIndex);
            }
        }
    }

    protected void bindParameter(DBRProgressMonitor monitor, PreparedStatement statement, DBSTypedObject paramType,
                                 int paramIndex, Object value) throws SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getValueType());
        } else {
            Number number = (Number)value;
            switch (paramType.getValueType()) {
            case java.sql.Types.BIGINT:
                statement.setLong(paramIndex, number.longValue());
                break;
            case java.sql.Types.FLOAT:
                statement.setFloat(paramIndex, number.floatValue());
                break;
            case java.sql.Types.INTEGER:
                statement.setInt(paramIndex, number.intValue());
                break;
            case java.sql.Types.SMALLINT:
                statement.setShort(paramIndex, number.shortValue());
                break;
            case java.sql.Types.TINYINT:
            case java.sql.Types.BIT:
                statement.setByte(paramIndex, number.byteValue());
                break;
            default:
                if (paramType.getScale() > 0) {
                    statement.setDouble(paramIndex, number.doubleValue());
                } else {
                    statement.setLong(paramIndex, number.longValue());
                }
                break;
            }
        }
    }

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            if (controller.getColumnMetaData().getValueType() == java.sql.Types.BIT) {
                Combo editor = new Combo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
                editor.add("0");
                editor.add("1");
                editor.setText(value == null ? "0" : value.toString());
                editor.setFocus();
                initInlineControl(controller, editor, new ValueExtractor<Combo>() {
                    public Object getValueFromControl(Combo control)
                    {
                        switch (control.getSelectionIndex()) {
                            case 0: return (byte)0;
                            case 1: return (byte)1;
                            default: return null;
                        }
                    }
                });
            } else {
                Text editor = new Text(controller.getInlinePlaceholder(), SWT.BORDER);
                editor.setText(value == null ? "" : value.toString());
                editor.setEditable(!controller.isReadOnly());
                editor.setTextLimit(MAX_NUMBER_LENGTH);
                editor.selectAll();
                editor.setFocus();
                editor.addVerifyListener(
                    controller.getColumnMetaData().getScale() == 0 ?
                        UIUtils.INTEGER_VERIFY_LISTENER :
                        UIUtils.NUMBER_VERIFY_LISTENER);

                initInlineControl(controller, editor, new ValueExtractor<Text>() {
                    public Object getValueFromControl(Text control)
                    {
                        String text = control.getText();
                        if (CommonUtils.isEmpty(text)) {
                            return null;
                        }
                        return convertStringToNumber(text, controller.getColumnMetaData());
                    }
                });
            }
            return true;
        } else {
            NumberViewDialog dialog = new NumberViewDialog(controller);
            dialog.open();
            return true;
        }
    }

    public Object copyValueObject(DBRProgressMonitor monitor, Object value)
        throws DBCException
    {
        // Number are immutable
        return value;
    }

    public void fillProperties(PropertySourceAbstract propertySource, DBDValueController controller)
    {
        propertySource.addProperty(
            "precision",
            "Precision",
            controller.getColumnMetaData().getPrecision());
        propertySource.addProperty(
            "scale",
            "Scale",
            controller.getColumnMetaData().getScale());
    }


    public static Number convertStringToNumber(String text, DBSTypedObject type)
    {
        if (text == null || text.length() == 0) {
            return null;
        }
        try {
            switch (type.getValueType()) {
            case java.sql.Types.BIGINT:
                return new Long(text);
            case java.sql.Types.DECIMAL:
                return new Double(text);
            case java.sql.Types.DOUBLE:
                return new Double(text);
            case java.sql.Types.FLOAT:
                return new Float(text);
            case java.sql.Types.INTEGER:
                return new Integer(text);
            case java.sql.Types.REAL:
                return new Double(text);
            case java.sql.Types.SMALLINT:
                return new Short(text);
            case java.sql.Types.TINYINT:
                return new Byte(text);
            default:
                if (type.getScale() > 0) {
                    return new Double(text);
                } else {
                    return new Long(text);
                }
            }
        }
        catch (NumberFormatException e) {
            log.error("Bad numeric value '" + text + "' - " + e.getMessage());
            return null;
        }
    }
}