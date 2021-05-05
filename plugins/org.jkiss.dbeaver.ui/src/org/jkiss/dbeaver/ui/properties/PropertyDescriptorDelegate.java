/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PropertyDescriptorDelegate
 */
public class PropertyDescriptorDelegate implements IPropertyDescriptor
{
    private static final ILabelProvider DEFAULT_LABEL_PROVIDER = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            if (element instanceof DBPNamedObject) {
                return DBUtils.getObjectFullName((DBPNamedObject) element, DBPEvaluationContext.UI);
            } else {
                return CommonUtils.toString(GeneralUtils.makeDisplayString(element));
            }
        }

        @Override
        public Image getImage(Object element) {
            if (element instanceof DBPObject) {
                DBPImage image = DBValueFormatting.getObjectImage((DBPObject) element, false);
/*
                if (image == null && element instanceof DBSObject) {
                    final DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().findNode((DBSObject) element);
                    if (node != null) {
                        image = node.getNodeIcon();
                    }
                }
*/
                if (image != null) {
                    return DBeaverIcons.getImage(image);
                }
            }
            return super.getImage(element);
        }
    };

    private final DBPPropertySource propSource;
    private final DBPPropertyDescriptor delegate;

    public PropertyDescriptorDelegate(DBPPropertySource propSource, DBPPropertyDescriptor delegate) {
        this.propSource = propSource;
        this.delegate = delegate;
    }

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        if (!delegate.isEditable(propSource.getEditableValue())) {
            return null;
        }
        return PropertyEditorUtils.createCellEditor(parent, propSource.getEditableValue(), delegate, SWT.SHEET);
    }

    @Override
    public String getCategory() {
        String category = delegate.getCategory();
        if (CommonUtils.isEmpty(category)) {
            category = DBConstants.CAT_MAIN;
        }
        return category;
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String[] getFilterFlags() {
        if (delegate.hasFeature(DBConstants.PROP_FEATURE_EXPENSIVE) ||
            delegate.hasFeature(DBConstants.PROP_FEATURE_HIDDEN) ||
            DBConstants.CAT_STATISTICS.equals(delegate.getCategory()))
        {
            return new String[] { IPropertySheetEntry.FILTER_ID_EXPERT };
        }
        return null;
    }

    @Override
    public Object getHelpContextIds() {
        return null;
    }

    @Override
    public Object getId() {
        return delegate.getId();
    }

    @Override
    public ILabelProvider getLabelProvider() {
        return DEFAULT_LABEL_PROVIDER;
    }

    @Override
    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
        return false;
    }
}