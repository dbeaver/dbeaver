package org.jkiss.dbeaver.ext.gaussdb.ui;

import java.awt.Composite;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDataSource;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreCharset;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablespace;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class GaussDBCreateDatabaseDialog extends BaseDialog {

    private final GaussDBDataSource dataSource;
    private List<PostgreRole>       allUsers;
    private List<PostgreCharset>    allEncodings;
    private List<PostgreTablespace> allTablespaces;

    private String                  name;
    private PostgreRole             owner;
    private String                  dbTemplate;
    private PostgreCharset          encoding;
    private PostgreTablespace       tablespace;
    private String                  compatibleMode;
    private Combo                   dbCompatibleMode;
    private Combo                   userCombo;
    private Combo                   encodinCombo;
    private Combo                   tablespaceCombo;

    public GaussDBCreateDatabaseDialog(Shell parentShell, GaussDBDataSource dataSource) {
        super(parentShell, PostgreMessages.dialog_create_db_title, null);
        this.dataSource = dataSource;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        boolean supportsRoles = dataSource.isServerVersionAtLeast(8, 1);
        boolean supportsEncodings = dataSource.getServerType().supportsEncodings();
        boolean supportsTablespaces = dataSource.getServerType().supportsTablespaces();

        final Composite composite = super.createDialogArea(parent);
        final Composite groupGeneral = UIUtils.createControlGroup(composite, PostgreMessages.dialog_create_db_group_general, 2,
                                                                  GridData.FILL_HORIZONTAL, SWT.NONE);
        final Text nameText = UIUtils.createLabelText(groupGeneral, PostgreMessages.dialog_create_db_label_db_name, "");
        nameText.addModifyListener(e -> {
            name = nameText.getText().trim();
            getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
        });
    }

    public GaussDBDataSource getDataSource() {
        return dataSource;
    }

    public List<PostgreRole> getAllUsers() {
        return allUsers;
    }

    public List<PostgreCharset> getAllEncodings() {
        return allEncodings;
    }

    public List<PostgreTablespace> getAllTablespaces() {
        return allTablespaces;
    }

    public String getName() {
        return name;
    }

    public PostgreRole getOwner() {
        return owner;
    }

    public String getDbTemplate() {
        return dbTemplate;
    }

    public PostgreCharset getEncoding() {
        return encoding;
    }

    public PostgreTablespace getTablespace() {
        return tablespace;
    }

    public String getCompatibleMode() {
        return compatibleMode;
    }

    public Combo getDbCompatibleMode() {
        return dbCompatibleMode;
    }

    public Combo getEncodinCombo() {
        return encodinCombo;
    }

    public Combo getTablespaceCombo() {
        return tablespaceCombo;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}