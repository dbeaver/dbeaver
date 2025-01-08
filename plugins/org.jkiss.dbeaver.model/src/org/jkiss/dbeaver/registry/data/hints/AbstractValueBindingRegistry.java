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

package org.jkiss.dbeaver.registry.data.hints;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorsRegistry
 */
public abstract class AbstractValueBindingRegistry<TYPE, DESC extends AbstractValueBindingDescriptor<TYPE>> {

    @NotNull
    protected abstract List<DESC> getDescriptors();

    @NotNull
    protected abstract TYPE getDefaultValueBinding();

    @NotNull
    public TYPE getValueBinding(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject type, @Nullable Class<?> valueType) {
        // Check starting from most restrictive to less restrictive
        TYPE valueBinding = findValueBinding(dataSource, type, valueType, true, true);
        if (valueBinding == null) {
            valueBinding = findValueBinding(dataSource, type, valueType, false, true);
        }
        if (valueBinding == null) {
            valueBinding = findValueBinding(dataSource, type, valueType, true, false);
        }
        if (valueBinding == null) {
            valueBinding = findValueBinding(dataSource, type, valueType, false, false);
        }
        if (valueBinding == null) {
            valueBinding = getDefaultValueBinding();
        }
        return valueBinding;
    }

    private TYPE findValueBinding(@Nullable DBPDataSource dataSource, DBSTypedObject typedObject, Class<?> valueType, boolean checkDataSource, boolean checkType) {
        for (DESC desc : getDescriptors()) {
            if (desc.supportsType(dataSource, typedObject, valueType, checkDataSource, checkType)) {
                return desc.getInstance();
            }
        }
        return null;
    }

    @NotNull
    public List<TYPE> getAllValueBindings(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject type, @Nullable Class<?> valueType) {
        List<TYPE> result = new ArrayList<>();
        for (DESC desc : getDescriptors()) {
            if (desc.supportsAnyType(dataSource, type, valueType)) {
                result.add(desc.getInstance());
            }
        }
        return result;
    }


}
