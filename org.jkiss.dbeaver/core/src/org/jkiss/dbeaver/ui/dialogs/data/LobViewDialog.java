/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.data.DBDValueController;

/**
 * TextViewDialog
 */
public class LobViewDialog extends ValueViewDialog {

    static final Log log = LogFactory.getLog(LobViewDialog.class);

    public LobViewDialog(DBDValueController valueController) {
        super(valueController);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

/*
        long lobLength = 0;
        if (lob != null) {
            try {
                lobLength = lob.getContentLength();
            } catch (DBCException e) {
                log.error("Can't obtain LOB information", e);
            }
        }

        {
            Label label = new Label(getInfoGroup(), SWT.NONE);
            label.setText("Content Length: ");
            Text text = new Text(getInfoGroup(), SWT.BORDER | SWT.READ_ONLY);
            text.setText(String.valueOf(lobLength));
        }
        if (lob instanceof DBDContentCharacter) {
            Label label = new Label(dialogGroup, SWT.NONE);
            label.setText("Content (sample 64k):");
            String content = null;
            try {
                DBDContentCharacter clob = (DBDContentCharacter)lob;
                content = clob.getString(0, lobLength >= 64000 ? 64000 : (int)lobLength);
            } catch (DBCException e) {
                log.warn("Error reading content", e);
            }
            Text clobText = new Text(dialogGroup, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
            clobText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
            clobText.setText(content);

            GridData ld = new GridData(GridData.FILL_BOTH);
            ld.heightHint = 200;
            ld.widthHint = 600;
            clobText.setLayoutData(ld);
        }

        {
            ToolBar toolBar = new ToolBar(dialogGroup, SWT.FLAT | SWT.HORIZONTAL);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            toolBar.setLayoutData(gd);
            UIUtils.createToolItem(toolBar, "Save", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    
                }
            });
            UIUtils.createToolItem(toolBar, "Load", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {

                }
            });
            if (lob instanceof DBDContentCharacter) {
                UIUtils.createToolItem(toolBar, "Copy to clipboard", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {

                    }
                });
            } else {
                UIUtils.createToolItem(toolBar, "View as text/binary", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {

                    }
                });
            }
        }
*/

        return dialogGroup;
    }

    protected Object getEditorValue()
    {
        return null;
    }

}