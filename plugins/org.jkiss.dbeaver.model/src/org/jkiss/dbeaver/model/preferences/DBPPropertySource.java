/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Property source
 */
public interface DBPPropertySource {

    Object getEditableValue();

    DBPPropertyDescriptor[] getProperties();

    Object getPropertyValue(@Nullable DBRProgressMonitor monitor, String id);

    boolean isPropertySet(String id);

    boolean isPropertyResettable(String id);

    void resetPropertyValue(@Nullable DBRProgressMonitor monitor, String id);

    void resetPropertyValueToDefault(String id);

    void setPropertyValue(@Nullable DBRProgressMonitor monitor, String id, Object value);

}
