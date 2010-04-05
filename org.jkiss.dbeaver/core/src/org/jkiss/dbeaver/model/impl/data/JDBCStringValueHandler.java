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
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;
import org.eclipse.jface.dialogs.MessageDialog;

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

}