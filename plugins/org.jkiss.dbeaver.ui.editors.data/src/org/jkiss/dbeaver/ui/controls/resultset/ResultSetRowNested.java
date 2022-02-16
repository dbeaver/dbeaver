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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingElement;

import java.util.Map;

public class ResultSetRowNested extends ResultSetRow {
    private final ResultSetRow parent;
    private final Map<DBDAttributeBinding, DBDAttributeBindingElement> elements;

    public ResultSetRowNested(int index, @NotNull ResultSetRow parent, @NotNull Object[] values, @NotNull Map<DBDAttributeBinding, DBDAttributeBindingElement> elements) {
        super(index, values);
        this.parent = parent;
        this.elements = elements;
    }

    @Nullable
    public DBDAttributeBindingElement getElement(@NotNull DBDAttributeBinding attribute) {
        return elements.get(attribute);
    }

    @NotNull
    public ResultSetRow getParent() {
        return parent;
    }
}
