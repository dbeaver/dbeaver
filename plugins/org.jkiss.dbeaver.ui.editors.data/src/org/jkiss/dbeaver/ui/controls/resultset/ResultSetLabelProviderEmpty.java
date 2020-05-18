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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.graphics.Color;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;

public class ResultSetLabelProviderEmpty implements IResultSetLabelProvider {

    public static final ResultSetLabelProviderEmpty INSTANCE = new ResultSetLabelProviderEmpty();

    @Nullable
    @Override
    public DBPImage getCellImage(DBDAttributeBinding attribute, ResultSetRow row) {
        return null;
    }

    @Nullable
    @Override
    public Color getCellForeground(DBDAttributeBinding attribute, ResultSetRow row) {
        return null;
    }

    @Nullable
    @Override
    public Color getCellBackground(DBDAttributeBinding attribute, ResultSetRow row) {
        return null;
    }
}
