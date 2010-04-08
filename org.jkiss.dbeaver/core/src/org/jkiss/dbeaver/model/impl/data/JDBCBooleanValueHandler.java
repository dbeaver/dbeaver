package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.DBException;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.SWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.jkiss.utils.CommonUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC number value handler
 */
public class JDBCBooleanValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCBooleanValueHandler INSTANCE = new JDBCBooleanValueHandler();

    static Log log = LogFactory.getLog(JDBCBooleanValueHandler.class);


    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            Object value = controller.getValue();

            Combo editor = new Combo(controller.getInlinePlaceholder(), SWT.READ_ONLY);
            editor.add("true");
            editor.add("false");
            editor.setText(value == null ? "false" : value.toString());
            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<Combo>() {
                public Object getValueFromControl(Combo control)
                {
                    switch (control.getSelectionIndex()) {
                        case 0: return Boolean.TRUE;
                        case 1: return Boolean.FALSE;
                        default: return null;
                    }
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
                dbStat.setBoolean(paramIndex + 1, (Boolean)value);
            }
        } catch (SQLException e) {
            throw new DBCException("Could not bind string parameter", e);
        }
    }

}