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

package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Property source which allows editing of multiple objects.
 */
public interface IPropertySourceMulti extends DBPPropertySource {

    boolean isPropertySet(Object object, ObjectPropertyDescriptor id);

    Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop, boolean formatValue);

    boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop);

    void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop);

    void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop, Object value)
        throws IllegalArgumentException;

}
