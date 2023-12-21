/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * EditConstraintPage
 *
 * @author Serge Rider
 */
public class EditConstraintPage extends AttributesSelectorPage {
    private static final Log log = Log.getLog(EditConstraintPage.class);

    private final DBSEntityConstraintType[] constraintTypes;
    private DBSEntityConstraintType selectedConstraintType;
    private String constraintExpression;
    private DBSEntityReferrer constraint;
    private Collection<? extends DBSEntityAttributeRef> attributes;
    private final ConstraintNameGenerator nameGenerator;

    private Group expressionGroup;
    private Text expressionText;
    private boolean enableConstraint = true;
    private boolean showEnable = false;
    private boolean useAllColumns = false;

    public EditConstraintPage(
        String title,
        DBSEntityConstraint constraint,
        DBSEntityConstraintType[] constraintTypes
    ) {
        this(title, constraint.getParentObject(), constraintTypes, false);
        if (constraint instanceof DBSEntityReferrer referrer) {
            this.constraint = referrer;
        }
    }

    public EditConstraintPage(
        String title,
        DBSEntity entity,
        DBSEntityConstraintType[] constraintTypes,
        boolean showEnable
    ) {
        super(title, entity);
        this.entity = entity;
        this.constraintTypes = constraintTypes;
        this.showEnable = showEnable;
        this.nameGenerator = new ConstraintNameGenerator(entity);
        Assert.isTrue(!ArrayUtils.isEmpty(this.constraintTypes));
    }

    public EditConstraintPage(String title, DBSEntityReferrer constraint) {
        super(title, constraint.getParentObject());
        this.constraint = constraint;
        this.constraintTypes = new DBSEntityConstraintType[]{constraint.getConstraintType()};
        try {
            this.attributes = constraint.getAttributeReferences(new VoidProgressMonitor());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(EditorsMessages.edit_constraints_error_title, EditorsMessages.edit_constraints_error_message, e);
        }
        this.nameGenerator = new ConstraintNameGenerator(constraint.getParentObject(), this.constraint.getName());
        if (constraint instanceof DBVEntityConstraint) {
            this.useAllColumns = ((DBVEntityConstraint) constraint).isUseAllColumns();
        }
    }

    private boolean isUniqueVirtualKeyEdit() {
        return this.constraintTypes.length == 1 && this.constraintTypes[0] == DBSEntityConstraintType.VIRTUAL_KEY;
    }

    @Override
    protected Composite createPageContents(Composite parent) {
        final Composite pageContents = super.createPageContents(parent);
        toggleEditAreas();
        return pageContents;
    }

    private void toggleEditAreas() {
        final boolean custom = selectedConstraintType.isCustom();
        columnsGroup.setVisible(!custom);
        ((GridData) columnsGroup.getLayoutData()).exclude = custom;
        expressionGroup.setVisible(custom);
        ((GridData) expressionGroup.getLayoutData()).exclude = !custom;
        columnsGroup.getParent().layout();
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel) {
        final Text nameText = entity != null ? UIUtils.createLabelText(
            panel,
            EditorsMessages.dialog_struct_edit_constrain_label_name,
            nameGenerator.getConstraintName()) : null;
        if (nameText != null) {
            nameText.selectAll();
            nameText.setFocus();
            nameText.addModifyListener(e -> nameGenerator.setConstraintName(nameText.getText().trim()));
        }

        UIUtils.createControlLabel(panel, EditorsMessages.dialog_struct_edit_constrain_label_type);
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (DBSEntityConstraintType constraintType : constraintTypes) {
            typeCombo.add(constraintType.getName());
            if (selectedConstraintType == null) {
                selectedConstraintType = constraintType;
            }
        }
        typeCombo.select(0);
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedConstraintType = constraintTypes[typeCombo.getSelectionIndex()];
                nameGenerator.setConstraintType(selectedConstraintType);
                if (nameText != null) {
                    nameText.setText(nameGenerator.getConstraintName());
                }
                nameGenerator.validateAllowedType(selectedConstraintType, EditConstraintPage.this);
                toggleEditAreas();
            }
        });

        if (showEnable) {
            final Button enableConstraintButton = UIUtils.createCheckbox(panel, EditorsMessages.edit_constraints_enable_constraint_text, EditorsMessages.edit_constraints_enable_constraint_tip, true, 2);
            enableConstraintButton.setVisible(showEnable);
            enableConstraintButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    enableConstraint = enableConstraintButton.getSelection();
                }
            });
        }

        if (isUniqueVirtualKeyEdit()) {
            final Button useAllColumnsCheck = UIUtils.createCheckbox(panel, EditorsMessages.edit_constraints_use_all_columns_text, EditorsMessages.edit_constraints_use_all_columns_tip, useAllColumns, 2);
            useAllColumnsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    useAllColumns = useAllColumnsCheck.getSelection();
                    columnsTable.setEnabled(!useAllColumns);
                    updatePageState();
                }
            });
        }
        nameGenerator.validateAllowedType(selectedConstraintType, this);
    }

    @Override
    protected void createContentsAfterColumns(Composite panel) {
        expressionGroup = UIUtils.createControlGroup(panel, EditorsMessages.edit_constraints_expression_text, 1, GridData.FILL_BOTH, 0);
        expressionText = new Text(expressionGroup, SWT.BORDER | SWT.MULTI);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = expressionText.getLineHeight() * 3;
        expressionText.setLayoutData(gd);
        expressionText.addModifyListener(e -> {
            constraintExpression = expressionText.getText();
            updatePageState();
        });
        columnsTable.setEnabled(!useAllColumns);
    }

    public String getConstraintName() {
        return nameGenerator.getConstraintName();
    }

    public DBSEntityConstraintType getConstraintType() {
        return selectedConstraintType;
    }

    public String getConstraintExpression() {
        return constraintExpression;
    }

    @Override
    public DBSObject getObject() {
        return constraint;
    }

    @Override
    public boolean isPageComplete() {
        if (selectedConstraintType == null) {
            return false;
        }
        if (selectedConstraintType.isCustom()) {
            return !CommonUtils.isEmpty(constraintExpression);
        } else {
            return useAllColumns || super.isPageComplete();
        }
    }

    @Override
    public boolean isColumnSelected(DBSEntityAttribute attribute) {
        if (!CommonUtils.isEmpty(attributes)) {
            for (DBSEntityAttributeRef ref : attributes) {
                if (ref.getAttribute() == attribute) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEnableConstraint() {
        return this.enableConstraint;
    }

    public boolean isUseAllColumns() {
        return this.useAllColumns;
    }
}
