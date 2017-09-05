/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * EditConstraintPage
 *
 * @author Serge Rider
 */
public class EditConstraintPage extends AttributesSelectorPage {

    private DBSEntity entity;
    private String constraintName;
    private DBSEntityConstraintType[] constraintTypes;
    private DBSEntityConstraintType selectedConstraintType;
    private String constraintExpression;
    private DBSEntityReferrer constraint;
    private Collection<? extends DBSEntityAttributeRef> attributes;

    private Map<DBSEntityConstraintType, String> TYPE_PREFIX = new HashMap<>();
    private Group expressionGroup;
    private Text expressionText;
    private Boolean enableConstraint = true;
    private Boolean showEnable = false;

    public EditConstraintPage(
        String title,
        DBSEntity entity,
        DBSEntityConstraintType[] constraintTypes)
    {
        super(title, entity);
        this.entity = entity;
        this.constraintTypes = constraintTypes;
        Assert.isTrue(!ArrayUtils.isEmpty(this.constraintTypes));
    }

    public EditConstraintPage(
            String title,
            DBSEntity entity,
            DBSEntityConstraintType[] constraintTypes, Boolean showEnable)
        {
            super(title, entity);
            this.entity = entity;
            this.constraintTypes = constraintTypes;
            this.showEnable = showEnable;
            Assert.isTrue(!ArrayUtils.isEmpty(this.constraintTypes));
        }

    public EditConstraintPage(
        String title,
        DBSEntityReferrer constraint)
    {
        super(title, constraint.getParentObject());
        this.constraint = constraint;
        this.constraintTypes = new DBSEntityConstraintType[] {constraint.getConstraintType()};
        try {
            this.attributes = constraint.getAttributeReferences(new VoidProgressMonitor());
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("Can't get attributes", "Error obtaining entity attributes", e);
        }
    }

    private void addTypePrefix(DBSEntityConstraintType type, String prefix) {
        if (entity.getDataSource() instanceof SQLDataSource) {
            prefix = ((SQLDataSource) entity.getDataSource()).getSQLDialect().storesUnquotedCase().transform(prefix);
        }
        TYPE_PREFIX.put(type, prefix);
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
        ((GridData)columnsGroup.getLayoutData()).exclude = custom;
        expressionGroup.setVisible(custom);
        ((GridData)expressionGroup.getLayoutData()).exclude = !custom;
        columnsGroup.getParent().layout();
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        if (entity != null) {
            addTypePrefix(DBSEntityConstraintType.PRIMARY_KEY, "_PK");
            addTypePrefix(DBSEntityConstraintType.UNIQUE_KEY, "_UN");
            addTypePrefix(DBSEntityConstraintType.VIRTUAL_KEY, "_VK");
            addTypePrefix(DBSEntityConstraintType.FOREIGN_KEY, "_FK");
            addTypePrefix(DBSEntityConstraintType.CHECK, "_CHECK");

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
                toggleEditAreas();
            }
        });
        
        final Button enableConstraintButton = UIUtils.createCheckbox(panel, "Enable Constraint", true);
        enableConstraintButton.setVisible(showEnable);
        enableConstraintButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e)
        	{
        		enableConstraint = enableConstraintButton.getSelection();
        	}
		});
    }

    @Override
    protected void createContentsAfterColumns(Composite panel) {
        expressionGroup = UIUtils.createControlGroup(panel, "Expression", 1, GridData.FILL_BOTH, 0);
        expressionText = new Text(expressionGroup, SWT.BORDER | SWT.MULTI);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = expressionText.getLineHeight() * 3;
        expressionText.setLayoutData(gd);
        expressionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                constraintExpression = expressionText.getText();
                updatePageState();
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

    public String getConstraintExpression() {
        return constraintExpression;
    }

    @Override
    public boolean isPageComplete() {
        if (selectedConstraintType == null) {
            return false;
        }
        if (selectedConstraintType.isCustom()) {
            return !CommonUtils.isEmpty(constraintExpression);
        } else {
            return super.isPageComplete();
        }
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
    
    public boolean isEnableConstraint()
    {
    	return this.enableConstraint;
    }

}
