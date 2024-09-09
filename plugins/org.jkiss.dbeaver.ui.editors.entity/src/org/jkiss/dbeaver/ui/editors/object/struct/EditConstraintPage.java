/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * EditConstraintPage
 *
 * @author Serge Rider
 */
public class EditConstraintPage extends AttributesSelectorPage<DBSEntity, DBSEntityAttribute> {
    private static final Log log = Log.getLog(EditConstraintPage.class);

    private DBSEntityConstraintType[] constraintTypes;
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

    public EditConstraintPage(String title, DBSEntityReferrer constraint) {
        super(title, constraint.getParentObject());
        this.constraint = constraint;

        if (object instanceof DBSEntityConstrainable entityConstrainable) {
            this.constraintTypes = entityConstrainable.getSupportedConstraints()
                .stream()
                .map(DBSEntityConstraintInfo::getType)
                .filter(type -> type != DBSEntityConstraintType.INDEX)
                .toArray(DBSEntityConstraintType[]::new);
        } else {
            this.constraintTypes = new DBSEntityConstraintType[]{constraint.getConstraintType()};
        }
        this.selectedConstraintType = constraint.getConstraintType();
        this.constraint = constraint;

        try {
            this.attributes = constraint.getAttributeReferences(new VoidProgressMonitor());
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(ObjectEditorMessages.edit_constraints_error_title, ObjectEditorMessages.edit_constraints_error_message, e);
        }
        this.nameGenerator = new ConstraintNameGenerator(
            constraint.getParentObject(),
            this.constraint.isPersisted() ? this.constraint.getName() : null,
            constraint.getConstraintType());
        if (constraint instanceof DBVEntityConstraint) {
            this.useAllColumns = ((DBVEntityConstraint) constraint).isUseAllColumns();
        }
    }

    private boolean isUniqueVirtualKeyEdit() {
        return this.constraintTypes.length == 1 && this.constraintTypes[0] == DBSEntityConstraintType.VIRTUAL_KEY;
    }

    public void setConstraintTypes(DBSEntityConstraintType[] constraintTypes) {
        this.constraintTypes = constraintTypes;
    }

    @Override
    protected Composite createPageContents(Composite parent) {
        final Composite pageContents = super.createPageContents(parent);
        toggleEditAreas();
        return pageContents;
    }

    @NotNull
    @Override
    protected List<? extends DBSEntityAttribute> getAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity object
    ) throws DBException {
        return CommonUtils.safeList(object.getAttributes(monitor));
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
        final Text nameText = object != null ? UIUtils.createLabelText(
            panel,
            ObjectEditorMessages.dialog_struct_edit_constrain_label_name,
            nameGenerator.getConstraintName()) : null;
        if (nameText != null) {
            nameText.selectAll();
            nameText.setFocus();
            nameText.addModifyListener(e -> {
                nameGenerator.setConstraintName(nameText.getText().trim());
                validateProperties();
            });
        }

        UIUtils.createControlLabel(panel, ObjectEditorMessages.dialog_struct_edit_constrain_label_type);
        final Combo typeCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (DBSEntityConstraintType constraintType : constraintTypes) {
            typeCombo.add(constraintType.getName());
            if (selectedConstraintType == null || constraintType == selectedConstraintType) {
                selectedConstraintType = constraintType;
                typeCombo.select(typeCombo.getItemCount() - 1);
            }
        }
        if (selectedConstraintType == null) {
            typeCombo.select(0);
            selectedConstraintType = constraintTypes[0];
        }
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedConstraintType = constraintTypes[typeCombo.getSelectionIndex()];
                nameGenerator.setConstraintType(selectedConstraintType);
                if (nameText != null) {
                    nameText.setText(nameGenerator.getConstraintName());
                }
                validateProperties();
                toggleEditAreas();
            }
        });

        if (showEnable) {
            final Button enableConstraintButton = UIUtils.createCheckbox(panel, ObjectEditorMessages.edit_constraints_enable_constraint_text, ObjectEditorMessages.edit_constraints_enable_constraint_tip, true, 2);
            enableConstraintButton.setVisible(showEnable);
            enableConstraintButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    enableConstraint = enableConstraintButton.getSelection();
                }
            });
        }

        if (isUniqueVirtualKeyEdit()) {
            final Button useAllColumnsCheck = UIUtils.createCheckbox(panel, ObjectEditorMessages.edit_constraints_use_all_columns_text, ObjectEditorMessages.edit_constraints_use_all_columns_tip, useAllColumns, 2);
            useAllColumnsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    useAllColumns = useAllColumnsCheck.getSelection();
                    columnsTable.setEnabled(!useAllColumns);
                    validateProperties();
                    updatePageState();
                }
            });
        }
        validateProperties();
    }

    @Override
    protected void createContentsAfterColumns(Composite panel) {
        expressionGroup = UIUtils.createControlGroup(panel, ObjectEditorMessages.edit_constraints_expression_text, 1, GridData.FILL_BOTH, 0);
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

    @Override
    protected boolean isColumnsRequired() {
        return !selectedConstraintType.isCustom() && !useAllColumns;
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
    protected String getEditError() {
        // Constraint name may be empty (auto-generated)
//        if (CommonUtils.isEmpty(constraint.getName())) {
//            return "Constraint name cannot be empty";
//        }

        String error = nameGenerator.validateAllowedType(selectedConstraintType);
        if (error != null) {
            return error;
        }
        return super.getEditError();
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

    public void performFinish() {
        if (constraint instanceof AbstractTableConstraint<?,?> atc) {
            atc.setConstraintType(this.getConstraintType());
            atc.setName(this.getConstraintName());
        }
    }

}
