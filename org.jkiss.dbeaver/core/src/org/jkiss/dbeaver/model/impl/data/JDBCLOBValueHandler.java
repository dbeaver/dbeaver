package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;

/**
 * JDBC string value handler
 */
public class JDBCLOBValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCLOBValueHandler INSTANCE = new JDBCLOBValueHandler();

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            return false;
        } else {
            return false;
        }
    }

}