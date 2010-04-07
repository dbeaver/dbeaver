package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.ui.controls.grid.IGridRow;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog {

    private IGridRow row;
    private DBCColumnMetaData columnInfo;
    private Composite infoGroup;

    protected ValueViewDialog(Shell shell, IGridRow row, DBCColumnMetaData columnInfo) {
        super(shell);
        this.row = row;
        this.columnInfo = columnInfo;
    }

    public IGridRow getRow()
    {
        return row;
    }

    public void setRow(IGridRow row)
    {
        this.row = row;
    }

    public Composite getInfoGroup() {
        return infoGroup;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        {
            infoGroup = new Composite(dialogGroup, SWT.NONE);
            GridData gd = new GridData();
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            infoGroup.setLayoutData(gd);
            infoGroup.setLayout(new GridLayout(2, false));
            Label label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Name: ");
            Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(columnInfo.getColumnName());

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Type: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(columnInfo.getTypeName());

            label = new Label(infoGroup, SWT.NONE);
            label.setText("Column Size: ");
            text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
            text.setText(String.valueOf(columnInfo.getDisplaySize()));
        }

        return dialogGroup;
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(columnInfo.getColumnName());
    }

}
