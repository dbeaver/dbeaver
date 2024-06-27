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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomFormEditor;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;
import org.jkiss.utils.CommonUtils;

public class PropertyObjectEditPage<OBJECT extends DBSObject> extends BaseObjectEditPage {

    private final OBJECT object;
    private final PropertySourceEditable propertySource;
    //private PropertyTreeViewer propertyViewer;
    private CustomFormEditor propertyEditor;

    public PropertyObjectEditPage(@Nullable DBECommandContext commandContext, @NotNull OBJECT object) {
        super("Edit " + object.getName());

        this.object = object;
        this.propertySource = new PropertySourceEditable(commandContext, object, object);
        this.propertySource.collectProperties();

        for (DBPPropertyDescriptor prop : propertySource.getProperties()) {
            if (prop instanceof ObjectPropertyDescriptor) {
                final ObjectPropertyDescriptor obj = (ObjectPropertyDescriptor) prop;
                if (!obj.isEditPossible(object) || obj.isNameProperty()) {
                    propertySource.removeProperty(prop);
                }
            }
        }
    }

    @NotNull
    public OBJECT getObject() {
        return object;
    }

    @Nullable
    public DBECommandContext getCommandContext() {
        return propertySource.getCommandContext();
    }

    protected String getPropertiesGroupTitle() {
        return null;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        final Composite composite;
        String groupTitle = getPropertiesGroupTitle();
        if (CommonUtils.isEmpty(groupTitle)) {
            composite = new Composite(parent, SWT.NONE);
        } else {
            composite = new Group(parent, SWT.NONE);
            ((Group)composite).setText(groupTitle);
        }
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createDefaultEditControls(composite);
        createAdditionalEditControls(parent);

        return composite;
    }

    protected void createAdditionalEditControls(Composite composite) {

    }

    private void createDefaultEditControls(Composite composite) {
        final Text nameText = UIUtils.createLabelText(composite, ObjectEditorMessages.dialog_struct_label_text_name, object.getName());
        nameText.selectAll();
        nameText.addModifyListener(e -> {
            if (object instanceof DBPNamedObject2 && object.getDataSource() != null) {
                String objectName = nameText.getText().trim();
                final String transformed = DBObjectNameCaseTransformer.transformName(object.getDataSource(), objectName);
                ((DBPNamedObject2) object).setName(transformed);
                validateProperties();
            }
        });

//        UIUtils
//            .createControlLabel(composite, ObjectEditorMessages.dialog_struct_label_text_properties)
//            .setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

        propertyEditor = new CustomFormEditor(getObject(), getCommandContext(), propertySource);
        for (DBPPropertyDescriptor prop : propertySource.getProperties()) {
            propertyEditor.createPropertyEditor(composite, prop);
        }

//        propertyViewer = new PropertyTreeViewer(composite, SWT.BORDER);
//        propertyViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).create());
//        propertyViewer.loadProperties(propertySource);
//        propertyViewer.addPropertyChangeListener(event -> validateProperties());
    }

    protected String getEditError() {
        if (CommonUtils.isEmpty(object.getName())) {
            return "Object name cannot be empty";
        }
        return super.getEditError();
    }

    @Override
    public void performFinish() throws DBException {
        // Save any active editors
        //propertyViewer.saveEditorValues();
    }
}
