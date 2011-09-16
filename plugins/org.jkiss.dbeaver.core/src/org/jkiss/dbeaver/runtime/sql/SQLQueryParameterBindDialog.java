package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import java.util.List;

/**
 * Parameter binding
 */
public class SQLQueryParameterBindDialog extends TrayDialog {

    private List<SQLStatementParameter> parameters;

    protected SQLQueryParameterBindDialog(Shell shell, List<SQLStatementParameter> parameters)
    {
        super(shell);
        this.parameters = parameters;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Bind parameter(s)");
        final Control composite = super.createDialogArea(parent);

        return composite;
    }
}
