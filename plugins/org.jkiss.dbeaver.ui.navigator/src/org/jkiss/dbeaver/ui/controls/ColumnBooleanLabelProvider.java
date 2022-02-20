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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.jkiss.dbeaver.ui.IColumnValueProvider;

class ColumnBooleanLabelProvider<ELEMENT, VALUE_TYPE> extends ColumnLabelProvider {
    private final IColumnValueProvider<ELEMENT, VALUE_TYPE> valueProvider;

    ColumnBooleanLabelProvider(IColumnValueProvider<ELEMENT, VALUE_TYPE> valueProvider) {
        this.valueProvider = valueProvider;
    }

    @Override
    public void update(ViewerCell cell) {
        // Do not set cell text
    }

    public IColumnValueProvider<ELEMENT, VALUE_TYPE> getValueProvider() {
        return valueProvider;
    }
}
