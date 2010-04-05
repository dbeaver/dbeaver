package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDValueAnnotation;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.SWT;

/**
 * Standard JDBC value handler
 */
public abstract class JDBCAbstractValueHandler implements DBDValueHandler {

    public Object getValueObject(DBCResultSet resultSet, int columnIndex)
        throws DBCException
    {
        return resultSet.getObject(columnIndex);
    }

    public DBDValueAnnotation[] getValueAnnotations(DBCColumnMetaData column)
        throws DBCException
    {
        return null;
    }

    protected static interface ValueExtractor <T extends Control> {
         Object getValueFromControl(T control);
    }
    protected <T extends Control> void initInlineControl(
        final DBDValueController controller,
        final T control,
        final ValueExtractor<T> extractor)
    {
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
        control.setFont(controller.getInlinePlaceholder().getFont());
        control.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e)
            {
                if (e.keyCode == SWT.CR) {
                    controller.updateValue(extractor.getValueFromControl(control));
                    controller.closeInlineEditor();
                } else if (e.keyCode == SWT.ESC) {
                    controller.closeInlineEditor();
                }
            }
            public void keyReleased(KeyEvent e)
            {
            }
        });
        control.setFocus();
    }

}