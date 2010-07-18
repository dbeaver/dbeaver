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
import org.jkiss.dbeaver.ui.UIUtils;

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

            org.eclipse.swt.widgets.List listFrom = new org.eclipse.swt.widgets.List(privsGroup, SWT.BORDER | SWT.SINGLE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 150;
            listFrom.setLayoutData(gd);

            Composite buttonsPane = new Composite(privsGroup, SWT.NONE);
            gd = new GridData(GridData.VERTICAL_ALIGN_CENTER);
            gd.minimumWidth = 50;
            buttonsPane.setLayoutData(gd);
            gl = new GridLayout(1, false);
            buttonsPane.setLayout(gl);

            Button btn1 = new Button(buttonsPane, SWT.PUSH);
            btn1.setText(">>");
            Button btn2 = new Button(buttonsPane, SWT.PUSH);
            btn2.setText("<<");

            org.eclipse.swt.widgets.List listTo = new org.eclipse.swt.widgets.List(privsGroup, SWT.BORDER | SWT.SINGLE);
            gd = new GridData(GridData.FILL_BOTH);
            gd.minimumWidth = 150;
            listTo.setLayoutData(gd);
        }
    }

    public void activatePart()
    {
        try {
            //
            getUser().getHost();
        }
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

}