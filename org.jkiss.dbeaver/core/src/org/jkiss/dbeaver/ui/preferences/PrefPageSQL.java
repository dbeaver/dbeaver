package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * PrefPageSQL
 */
public class PrefPageSQL extends PreferencePage implements IWorkbenchPreferencePage
{
    public PrefPageSQL()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected Control createContents(Composite parent)
    {
/*
        Group scriptsGroup = new Group(parent, SWT.NONE);
        scriptsGroup.setText("Statements");
        scriptsGroup.setLayout(new GridLayout(2, false));

        {
            Label rsSize = new Label(scriptsGroup, SWT.NONE);
            rsSize.setText("ResultSet maximum size:");

            Text rsSizeText = new Text(scriptsGroup, SWT.BORDER);
        }

        {
            Label acEnabled = new Label(scriptsGroup, SWT.NONE);
            acEnabled.setText("Auto-commit by default:");

            Button acCheck = new Button(scriptsGroup, SWT.CHECK);
            acCheck.setText("Enabled");
        }

        {
            Label executeTimeoutLabel = new Label(scriptsGroup, SWT.NONE);
            executeTimeoutLabel.setText("SQL statement timeout:");

            Text executeTimeoutText = new Text(scriptsGroup, SWT.BORDER);
        }
*/

        return parent;
    }

    public void init(IWorkbench workbench)
    {
    }

}