/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceAbstract;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

public class AttributeEditPage extends BaseObjectEditPage {

    private DBSEntityAttribute attribute;
    private final DBECommandContext commandContext;
    private PropertyTreeViewer propertyViewer;

    public AttributeEditPage(@Nullable DBECommandContext commandContext, @NotNull DBSEntityAttribute attribute)
    {
        super(NLS.bind(EditorsMessages.dialog_struct_attribute_edit_page_header_edit_attribute, DBUtils.getObjectFullName(attribute, DBPEvaluationContext.UI)));
        this.commandContext = commandContext;
        this.attribute = attribute;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        final Text nameText = UIUtils.createLabelText(propsGroup, EditorsMessages.dialog_struct_attribute_edit_page_label_text_name, attribute.getName()); //$NON-NLS-2$
        nameText.addModifyListener(e -> {
            if (attribute instanceof DBPNamedObject2) {
                ((DBPNamedObject2) attribute).setName(DBObjectNameCaseTransformer.transformName(attribute.getDataSource(), nameText.getText().trim()));
            }
        });

        UIUtils.createControlLabel(propsGroup, EditorsMessages.dialog_struct_attribute_edit_page_label_text_properties).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        propertyViewer = new PropertyTreeViewer(propsGroup, SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        propertyViewer.getControl().setLayoutData(gd);
        propertyViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return true;
            }
        });

        PropertySourceAbstract pc = new PropertySourceEditable(commandContext, attribute, attribute) {
            @Override
            public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object editableValue, ObjectPropertyDescriptor prop, Object newValue) throws IllegalArgumentException {
                super.setPropertyValue(monitor, editableValue, prop, newValue);

/*
                if (prop.getId().equals("dataType")) {
                    newValue = getPropertyValue(monitor, editableValue, prop);
                    if (newValue instanceof DBSDataType) {
                        DBPPropertyDescriptor lengthProp = getProperty("maxLength");
                        if (lengthProp instanceof ObjectPropertyDescriptor) {
                            DBPDataKind dataKind = ((DBSDataType) newValue).getDataKind();
                            if (dataKind == DBPDataKind.STRING) {
                                setPropertyValue(monitor, editableValue, (ObjectPropertyDescriptor) lengthProp, 100);
                            } else {
                                setPropertyValue(monitor, editableValue, (ObjectPropertyDescriptor) lengthProp, null);
                            }
                            propertyViewer.update(lengthProp, null);
                        }
                    }
                }
*/
            }
        };
        pc.collectProperties();
        for (DBPPropertyDescriptor prop : pc.getProperties()) {
            if (prop instanceof ObjectPropertyDescriptor) {
                if (((ObjectPropertyDescriptor) prop).isEditPossible() && !((ObjectPropertyDescriptor) prop).isNameProperty()) {
                    continue;
                }
            }
            pc.removeProperty(prop);
        }
        propertyViewer.loadProperties(pc);

        return propsGroup;
    }

    @Override
    public void performFinish() throws DBException {
        // Save any active editors
        propertyViewer.saveEditorValues();
    }
}
