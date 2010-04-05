package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Standard JDBC value handler
 */
public class JDBCUnsupportedValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCUnsupportedValueHandler INSTANCE = new JDBCUnsupportedValueHandler();

    public boolean editValue(DBDValueController controller)
        throws DBException
    {
        if (!controller.isInlineEdit()) {
            controller.showMessage(
                "No suitable editor found for type '" + controller.getColumnMetaData().getTypeName() + "'", true);
        }
        return false;
    }

}
