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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
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
                    final DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().findNode((DBSObject) element);
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
        return UIUtils.createCellEditor(parent, propSource.getEditableValue(), delegate);
    }

    @Override
    public String getCategory() {
        return delegate.getCategory();
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