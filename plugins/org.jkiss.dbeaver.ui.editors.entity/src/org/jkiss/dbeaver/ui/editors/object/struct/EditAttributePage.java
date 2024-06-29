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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntityConstrainable;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintInfo;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt2;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EditAttributePage
 */
public class EditAttributePage extends PropertyObjectEditPage<DBSTableColumn> {

    @NotNull
    private final Map<String, Object> options;
    private boolean isUnique;
    private List<DBSEntityConstraintInfo> constraintTypes;

    private DBSEntityConstraintInfo selectedConstraintType;
    private final ConstraintNameGenerator constraintNameGenerator;
    private Combo keyTypeCombo;
    private Text constraintNameText;

    public EditAttributePage(
        @Nullable DBECommandContext commandContext,
        @NotNull DBSTableColumn object,
        @NotNull Map<String, Object> options
    ) {
        super(commandContext, object);
        setTitle(NLS.bind(
            ObjectEditorMessages.dialog_struct_attribute_edit_page_header_edit_attribute,
            DBUtils.getObjectFullName(object, DBPEvaluationContext.UI)
        ));
        this.options = options;
        this.constraintNameGenerator = new ConstraintNameGenerator(object.getParentObject());
    }

    @Override
    protected String getPropertiesGroupTitle() {
        return ObjectEditorMessages.dialog_struct_label_text_properties;
    }

    @Override
    protected void createAdditionalEditControls(Composite composite) {
        if (getObject().getParentObject() instanceof DBSEntityConstrainable ec) {
            createKeysGroup(composite, ec);
        }
    }

    private void createKeysGroup(Composite composite, DBSEntityConstrainable attributeConstrainable) {
        constraintTypes = attributeConstrainable.getSupportedConstraints();
        if (CommonUtils.isEmpty(constraintTypes)) {
            return;
        }
        constraintTypes = constraintTypes.stream()
            .filter(ct -> AbstractTableConstraint.class.isAssignableFrom(ct.getImplClass()) && ct.getType().isUnique())
            .collect(Collectors.toList());
        if (CommonUtils.isEmpty(constraintTypes)) {
            return;
        }

        Group keysGroup = UIUtils.createControlGroup(composite, "Keys", 3, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        Button uniqueCheck = UIUtils.createCheckbox(keysGroup, "Unique", "Mark column unique", false, 1);

        keyTypeCombo = UIUtils.createLabelCombo(keysGroup, "Type", "Constraint type", SWT.DROP_DOWN | SWT.READ_ONLY);
        keyTypeCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (DBSEntityConstraintInfo ct : constraintTypes) {
            keyTypeCombo.add(ct.getType().getLocalizedName());
        }
        keyTypeCombo.setEnabled(false);

        uniqueCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
            isUnique = uniqueCheck.getSelection();
            keyTypeCombo.setEnabled(isUnique);
            constraintNameText.setEnabled(isUnique);
            if (isUnique && keyTypeCombo.getSelectionIndex() < 0) {
                keyTypeCombo.select(0);
            }
            updateConstraintType();
        }));
        keyTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateConstraintType();
            }
        });

        UIUtils.createEmptyLabel(keysGroup, 1, 1);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = 150;
        constraintNameText = UIUtils.createLabelText(keysGroup, "Name", null, SWT.BORDER, gd);
        constraintNameText.addModifyListener(e -> constraintNameGenerator.setConstraintName(constraintNameText.getText()));
    }

    private void updateConstraintType() {
        int selectionIndex = keyTypeCombo.getSelectionIndex();
        if (!isUnique || selectionIndex < 0) {
            selectedConstraintType = null;
        } else {
            selectedConstraintType = constraintTypes.get(selectionIndex);
            constraintNameGenerator.setConstraintType(selectedConstraintType.getType());
            constraintNameText.setText(constraintNameGenerator.getConstraintName());
        }
        validateProperties();
    }

    @Override
    protected String getEditError() {
        if (isUnique) {
            if (selectedConstraintType == null) {
                return "You must choose constraint type";
            }
            String error = constraintNameGenerator.validateAllowedType(selectedConstraintType.getType());
            if (error != null) {
                return error;
            }
        }
        return super.getEditError();
    }

    @Override
    public void performFinish() throws DBException {
        super.performFinish();

        if (isUnique && selectedConstraintType != null) {
            options.put(SQLObjectEditor.OPTION_ADDITIONAL_ACTION, (DBRRunnableWithProgress) monitor -> {
                try {
                    createConstrainForColumn(monitor);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
    }

    private void createConstrainForColumn(DBRProgressMonitor monitor) throws DBException {
        // Create constraint
        DBECommandContext commandContext = getCommandContext();

        DBEObjectManager<?> objectManager = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(
            selectedConstraintType.getImplClass());
        if (objectManager == null) {
            throw new DBException("Object manager not found for type '" + selectedConstraintType.getImplClass().getName() + "'");
        }
        DBEObjectMaker<?,?> objectMaker = (DBEObjectMaker<?,?>) objectManager;
        Map<String, Object> constrOptions = new LinkedHashMap<>(options);
        constrOptions.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        DBSTableColumn column = getObject();
        if (column instanceof DBSTypedObjectExt2 to2) {
            to2.setRequired(true);
        }
        DBSObject newConstraint = objectMaker.createNewObject(
            monitor,
            commandContext,
            column.getParentObject(),
            null,
            constrOptions);
        if (newConstraint instanceof AbstractTableConstraint<?,?> atc) {
            atc.setName(constraintNameGenerator.getConstraintName());
            atc.setConstraintType(selectedConstraintType.getType());
            atc.addAttributeReference(column);
        }
    }
}
