/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.PairListControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PrivilegesPairList
 */
public class PrivilegesPairList extends PairListControl<String> {

    public PrivilegesPairList(Composite parent) {
        super(parent, SWT.NONE, "Available", "Granted");
    }

    public void setModel(Map<String, Boolean> privs)
    {
        List<String> availPrivs = new ArrayList<String>();
        List<String> grantedPrivs = new ArrayList<String>();
        for (Map.Entry<String,Boolean> priv : privs.entrySet()) {
            if (priv.getValue()) {
                grantedPrivs.add(priv.getKey());
            } else {
                availPrivs.add(priv.getKey());
            }
        }
        super.setModel(availPrivs, grantedPrivs);
    }
}
