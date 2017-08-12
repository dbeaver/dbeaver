package org.jkiss.dbeaver.ext.exasol.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.exasol.manager.security.ExasolGrantee;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;


public class ExasolCreateSchemaDialog extends BaseDialog {

    private List<ExasolGrantee> grantees;
    private String name;
    private ExasolDataSource datasource;
    private ExasolGrantee owner;

    
    public ExasolCreateSchemaDialog(Shell parentShell, ExasolDataSource datasource) {
        super(parentShell,"Create schema",null);
        this.datasource = datasource;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected Composite createDialogArea(Composite parent)
    {
        final Composite composite = super.createDialogArea(parent);
        
        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        final Text nameText = UIUtils.createLabelText(group, "Schema Name", "");
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            }
        });

        final Combo userCombo = UIUtils.createLabelCombo(group, "Owner", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);

        userCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                owner = grantees.get(userCombo.getSelectionIndex());
            }
        });
        
        new AbstractJob("Load users") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    final List<String> granteeNames = new ArrayList<>();
                    grantees = new ArrayList<>(datasource.getAllGrantees(monitor));
                    
                    DBeaverUI.syncExec(new Runnable() {
                        @Override
                        public void run()
                        {
                            for (ExasolGrantee grantee: grantees)
                            {
                                String name = grantee.getName();
                                userCombo.add(name);
                            }
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        
        return composite;


    }
    
    public String getName() {
        return name;
    }
    
    public ExasolGrantee getOwner() {
        return owner;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
}
