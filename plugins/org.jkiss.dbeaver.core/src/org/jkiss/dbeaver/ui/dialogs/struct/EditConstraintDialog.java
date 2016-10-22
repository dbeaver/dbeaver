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

package org.jkiss.dbeaver.ui.dialogs.struct;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * EditConstraintDialog
 *
 * @author Serge Rider
 */
public class EditConstraintDialog extends AttributesSelectorDialog {

    private DBSEntity entity;
    private String constraintName;
    private DBSEntityConstraintType[] constraintTypes;
    private DBSEntityConstraintType selectedConstraintType;
    private DBSEntityReferrer constraint;
    private Collection<? extends DBSEntityAttributeRef> attributes;

    private Map<DBSEntityConstraintType, String> TYPE_PREFIX = new HashMap<>();

    public EditConstraintDialog(
        Shell shell,
        String title,
        DBSEntity entity,
        DBSEntityConstraintType[] constraintTypes)
    {
        super(shell, title, entity);
        this.entity = entity;
        this.constraintTypes = constraintTypes;
        Assert.isTrue(!ArrayUtils.isEmpty(this.constraintTypes));
    }

    public EditConstraintDialog(
        Shell shell,
        String title,
        DBSEntityReferrer constraint)
    {
        super(shell, title, constraint.getParentObject());
        this.constraint = constraint;
        this.constraintTypes = new DBSEntityConstraintType[] {constraint.getConstraintType()};
        try {
            this.attributes = constraint.getAttributeReferences(VoidProgressMonitor.INSTANCE);
        } catch (DBException e) {
            UIUtils.showErrorDialog(shell, "Can't get attributes", "Error obtaining entity attributes", e);
        }
    }

    private void addTypePrefix(DBSEntityConstraintType type, String prefix) {
        if (entity.getDataSource() instanceof SQLDataSource) {
            prefix = ((SQLDataSource) entity.getDataSource()).getSQLDialect().storesUnquotedCase().transform(prefix);
        }
        TYPE_PREFIX.put(type, prefix);
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        if (entity != null) {
            addTypePrefix(DBSEntityConstraintType.PRIMARY_KEY, "_PK");
            addTypePrefix(DBSEntityConstraintType.UNIQUE_KEY, "_UN");
            addTypePrefix(DBSEntityConstraintType.VIRTUAL_KEY, "_VK");
            addTypePrefix(DBSEntityConstraintType.FOREIGN_KEY, "_FK");

            String namePrefix = TYPE_PREFIX.get(constraintTypes[0]);
            if (namePrefix == null) {
                namePrefix = "KEY";
            }
            this.constraintName = DBObjectNameCaseTransformer.transformName(entity.getDataSource(), CommonUtils.escapeIdentifier(entity.getName()) + namePrefix);
        }

        final Text nameText = entity != null ? UIUtils.createLabelText(panel, "Name", constraintName) : null;
        if (nameText != null) {
            nameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    constraintName = nameText.getText();
                }
            });
        }

        UIUtils.createControlLabel(panel, CoreMessages.dialog_struct_edit_constrain_label_type);
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (DBSEntityConstraintType constraintType : constraintTypes) {
            typeCombo.add(constraintType.getName());
            if (selectedConstraintType == null) {
                selectedConstraintType = constraintType;
            }
        }
        typeCombo.select(0);
        typeCombo.setEnabled(constraintTypes.length > 1);
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                DBSEntityConstraintType oldType = selectedConstraintType;
                selectedConstraintType = constraintTypes[typeCombo.getSelectionIndex()];
                if (constraintName != null) {
                    String oldPrefix = TYPE_PREFIX.get(oldType);
                    if (oldPrefix != null && constraintName.endsWith(oldPrefix)) {
                        String newPrefix = TYPE_PREFIX.get(selectedConstraintType);
                        if (newPrefix != null) {
                            constraintName = constraintName.substring(0, constraintName.length() - oldPrefix.length()) + newPrefix;
                            if (nameText != null) {
                                nameText.setText(constraintName);
                            }
                        }
                    }
                }
            }
        });
    }

    public String getConstraintName() {
        return constraintName;
    }

    public DBSEntityConstraintType getConstraintType()
    {
        return selectedConstraintType;
    }

    @Override
    public boolean isColumnSelected(DBSEntityAttribute attribute)
    {
        if (!CommonUtils.isEmpty(attributes)) {
            for (DBSEntityAttributeRef ref : attributes) {
                if (ref.getAttribute() == attribute) {
                    return true;
                }
            }
        }
        return false;
    }

}
