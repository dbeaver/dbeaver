package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.DBException;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.SWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.jkiss.utils.CommonUtils;

/**
 * JDBC number value handler
 */
public class JDBCNumberValueHandler extends JDBCAbstractValueHandler {

    public static final JDBCNumberValueHandler INSTANCE = new JDBCNumberValueHandler();

    static Log log = LogFactory.getLog(JDBCNumberValueHandler.class);

    private static final int MAX_NUMBER_LENGTH = 100;

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
                Text editor = new Text(controller.getInlinePlaceholder(), SWT.NONE);
                editor.setText(value == null ? "" : value.toString());
                editor.setEditable(!controller.isReadOnly());
                editor.setTextLimit(MAX_NUMBER_LENGTH);
                editor.selectAll();
                editor.setFocus();

                editor.addListener(SWT.Verify, new Listener() {
                    public void handleEvent(Event e)
                    {
                        for (int i = 0; i < e.text.length(); i++) {
                            char ch = e.text.charAt(i);
                            if (!Character.isDigit(ch) && ch != '.' && ch != '-') {
                                e.doit = false;
                                return;
                            }
                        }
                    }
                });

                initInlineControl(controller, editor, new ValueExtractor<Text>() {
                    public Object getValueFromControl(Text control)
                    {
                        String text = control.getText();
                        if (CommonUtils.isEmpty(text)) {
                            return null;
                        }
                        try {
                            switch (controller.getColumnMetaData().getValueType()) {
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
                            case java.sql.Types.NUMERIC:
                                return new Double(text);
                            case java.sql.Types.REAL:
                                return new Double(text);
                            case java.sql.Types.SMALLINT:
                                return new Short(text);
                            case java.sql.Types.TINYINT:
                                return new Byte(text);
                            default:
                                return new Double(text);
                            }
                        }
                        catch (NumberFormatException e) {
                            log.error("Bad numeric value '" + text + "' - " + e.getMessage());
                            return controller.getValue();
                        }
                    }
                });
            }
            return true;
        } else {
            return false;
        }
    }

}