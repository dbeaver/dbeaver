/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.registry.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;

/**
 * Object filter edit dialog
 */
public class EditObjectFilterDialog extends HelpEnabledDialog {

    static final Log log = LogFactory.getLog(EditObjectFilterDialog.class);

    private String objectTitle;
    private DBSObjectFilter filter;
    private Composite blockControl;
    private ControlEnableState blockEnableState;

    protected EditObjectFilterDialog(Shell shell, String objectTitle, DBSObjectFilter filter)
    {
        super(shell, IHelpContextIds.CTX_EDIT_OBJECT_FILTERS);
        this.objectTitle = objectTitle;
        this.filter = new DBSObjectFilter(filter);
    }

    public DBSObjectFilter getFilter()
    {
        return filter;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(objectTitle + " Filter");
        //getShell().setImage(DBIcon.EVENT.getImage());

        Composite composite = (Composite) super.createDialogArea(parent);

        return composite;
    }


    protected void enableHandlerContent(NetworkHandlerDescriptor descriptor)
    {
        if (filter.isEnabled()) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else if (blockEnableState == null) {
            blockEnableState = ControlEnableState.disable(blockControl);
        }
    }

    private void saveConfigurations()
    {
    }

    @Override
    protected void okPressed()
    {
        saveConfigurations();
        super.okPressed();
    }

    @Override
    protected void cancelPressed()
    {
        super.cancelPressed();
    }
}
