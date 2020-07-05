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
package org.jkiss.dbeaver.model.preferences;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Property descriptor.
 */
public interface DBPPropertyDescriptor {

    /**
     * Returns the id for this property. This object is used internally to distinguish one property descriptor from another.
     */
    @NotNull
    String getId();

    /**
     * Returns the name of the category to which this property belongs. Properties
     * belonging to the same category are grouped together visually. This localized
     * string is shown to the user
     */
    @Nullable
    String getCategory();

    /**
     * Returns the display name for this property. This localized string is shown to
     * the user as the name of this property.
     *
     * @return a displayable name
     */
    @NotNull
    String getDisplayName();

    /**
     * Returns a brief description of this property. This localized string is shown
     * to the user when this property is selected.
     */
    @Nullable
    String getDescription();

    /**
     * Returns the type of this property. Types is a java class.
     */
    @Nullable
    Class<?> getDataType();

    boolean isRequired();

    @Nullable
    Object getDefaultValue();

    // TODO: remove "object" parameter
    boolean isEditable(Object object);

}
