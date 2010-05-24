/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.ui.DBIcon;

import java.text.NumberFormat;

/**
 * UI Utils
 */
public class UIUtils {

    public static final VerifyListener INTEGER_VERIFY_LISTENER = new VerifyListener() {
        public void verifyText(VerifyEvent e)
        {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && ch != '-' && ch != '+') {
                    e.doit = false;
                    return;
                }
            }
        }
    };

    public static final VerifyListener NUMBER_VERIFY_LISTENER = new VerifyListener() {
        public void verifyText(VerifyEvent e)
        {
            for (int i = 0; i < e.text.length(); i++) {
                char ch = e.text.charAt(i);
                if (!Character.isDigit(ch) && ch != '.' && ch != '-') {
                    e.doit = false;
                    return;
                }
            }
        }
    };

    public static Object makeStringForUI(Object object)
    {
        if (object == null) {
            return "";
        }
        if (object instanceof Number) {
            return NumberFormat.getInstance().format(object);
        }
        return object;
    }

    public static ToolItem createToolItem(ToolBar toolBar, String text, DBIcon icon, SelectionListener listener)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setToolTipText(text);
        item.setImage(icon.getImage());
        item.addSelectionListener(listener);
        return item;
    }

    public static ToolItem createToolItem(ToolBar toolBar, String text, DBIcon icon, final IAction action)
    {
        ToolItem item = new ToolItem(toolBar, SWT.PUSH);
        item.setToolTipText(text);
        item.setImage(icon.getImage());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                action.run();
            }
        });
        return item;
    }

}
