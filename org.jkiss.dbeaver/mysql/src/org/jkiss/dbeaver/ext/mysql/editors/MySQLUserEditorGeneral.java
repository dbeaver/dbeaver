/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegesPairList;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PairListControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MySQLUserEditorGeneral
 */
public class MySQLUserEditorGeneral extends MySQLUserEditorAbstract
{
    static final Log log = LogFactory.getLog(MySQLUserEditorGeneral.class);

    public void createPartControl(Composite parent)
    {
        GridLayout gl = new GridLayout(2, false);
        parent.setLayout(gl);

        {
            Composite loginGroup = UIUtils.createControlGroup(parent, "Login", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            UIUtils.createLabelText(loginGroup, "Username", getUser().getName());
            UIUtils.createLabelText(loginGroup, "Host", getUser().getHost());

            UIUtils.createLabelText(loginGroup, "Password", "", SWT.BORDER | SWT.PASSWORD);
            UIUtils.createLabelText(loginGroup, "Confirm", "", SWT.BORDER | SWT.PASSWORD);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(parent, "Limits", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            UIUtils.createLabelText(limitsGroup, "Max Queries", "" + getUser().getMaxQuestions());
            UIUtils.createLabelText(limitsGroup, "Max Updates", "" + getUser().getMaxUpdates());
            UIUtils.createLabelText(limitsGroup, "Max Connections", "" + getUser().getMaxConnections());
            UIUtils.createLabelText(limitsGroup, "Max User Connections", "" + getUser().getMaxUserConnections());
        }


        {
            Composite privsGroup = UIUtils.createControlGroup(parent, "Privileges", 3, GridData.FILL_VERTICAL, 0);
            GridData gd = (GridData)privsGroup.getLayoutData();
            gd.horizontalSpan = 2;
            gd.widthHint = 400;

            PrivilegesPairList privPair = new PrivilegesPairList(privsGroup);
            privPair.setModel(getUser().getGlobalPrivileges());
        }
    }

    public void activatePart()
    {

    }

}
