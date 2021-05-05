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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.utils.CommonUtils;

/**
 * PropertySourceDelegate
 */
public class PropertySourceDelegate implements IPropertySource2
{
    private final DBPPropertySource source;

    public PropertySourceDelegate(DBPPropertySource source) {
        this.source = source;
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return source.isPropertyResettable(CommonUtils.toString(id));
    }

    @Override
    public Object getEditableValue() {
        return source.getEditableValue();
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        DBPPropertyDescriptor[] src = source.getProperties();
        if (src == null) {
            return null;
        }
        IPropertyDescriptor[] dst = new IPropertyDescriptor[src.length];
        for (int i = 0; i < src.length; i++) {
            dst[i] = new PropertyDescriptorDelegate(source, src[i]);
        }
        return dst;
    }

    @Override
    public Object getPropertyValue(Object id) {
        Object value = source.getPropertyValue(null, CommonUtils.toString(id));
        if (value instanceof DBPNamedObject) {
            return ((DBPNamedObject) value).getName();
        }
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isPropertySet(Object id) {
        return source.isPropertySet(CommonUtils.toString(id));
    }

    @Override
    public void resetPropertyValue(Object id) {
        source.resetPropertyValue(null, CommonUtils.toString(id));
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        source.setPropertyValue(null, CommonUtils.toString(id), value);
    }
}