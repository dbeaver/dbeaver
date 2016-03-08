/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCharset;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCollation;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * MySQLCreateDatabaseDialog
 */
public class MySQLCreateDatabaseDialog extends BaseDialog
{
    public static final String DEFAULT_CHARSET_NAME = "utf8";
    private final MySQLDataSource dataSource;
    private String name;
    private MySQLCharset charset;
    private MySQLCollation collation;

    public MySQLCreateDatabaseDialog(Shell parentShell, MySQLDataSource dataSource) {
        super(parentShell, "Create database", null);
        this.dataSource = dataSource;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));

        final Text nameText = UIUtils.createLabelText(group, "Database name", "");
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                name = nameText.getText();
                getButton(IDialogConstants.OK_ID).setEnabled(!name.isEmpty());
            }
        });

        final Combo charsetCombo = UIUtils.createLabelCombo(group, "Charset", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MySQLCharset cs : dataSource.getCharsets()) {
            charsetCombo.add(cs.getName());
        }
        charsetCombo.setText(DEFAULT_CHARSET_NAME);
        charset = dataSource.getCharset(DEFAULT_CHARSET_NAME);
        assert charset != null;

        final Combo collationCombo = UIUtils.createLabelCombo(group, "Collation", SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MySQLCollation col : charset.getCollations()) {
            collationCombo.add(col.getName());
        }
        collation = charset.getDefaultCollation();
        if (collation != null) {
            UIUtils.setComboSelection(collationCombo, collation.getName());
        }

        charsetCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                charset = dataSource.getCharset(charsetCombo.getText());
                assert charset != null;

                collationCombo.removeAll();
                for (MySQLCollation col : charset.getCollations()) {
                    collationCombo.add(col.getName());
                }
                collation = charset.getDefaultCollation();
                if (collation != null) {
                    UIUtils.setComboSelection(collationCombo, collation.getName());
                }
            }
        });
        collationCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                collation = charset.getCollation(collationCombo.getText());
            }
        });

        return composite;
    }

    public String getName() {
        return name;
    }

    public MySQLCharset getCharset() {
        return charset;
    }

    public MySQLCollation getCollation() {
        return collation;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}
