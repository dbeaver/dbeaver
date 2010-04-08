package org.jkiss.dbeaver.model.impl.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC string value handler
 */
public class JDBCStringValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCStringValueHandler INSTANCE = new JDBCStringValueHandler();
    private static final int MAX_STRING_LENGTH = 0xffff;

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            Text editor = new Text(controller.getInlinePlaceholder(), SWT.NONE);
            editor.setText(value == null ? "" : value.toString());
            editor.setEditable(!controller.isReadOnly());
            editor.setTextLimit(MAX_STRING_LENGTH);
            editor.selectAll();
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<Text>() {
                public Object getValueFromControl(Text control)
                {
                    return control.getText();
                }
            });
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void bindParameter(DBCStatement statement, DBSTypedObject columnMetaData, int paramIndex, Object value) throws DBCException
    {
        PreparedStatement dbStat = getPreparedStatement(statement);
        try {
            if (value == null) {
                dbStat.setNull(paramIndex + 1, columnMetaData.getValueType());
            } else {
                dbStat.setString(paramIndex + 1, value.toString());
            }
        } catch (SQLException e) {
            throw new DBCException("Could not bind string parameter", e);
        }
    }

}