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

package org.jkiss.dbeaver.model.data.hints;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDResultSetModel;
import org.jkiss.dbeaver.model.data.DBDValueRow;

import java.util.EnumSet;

/**
 * Row hint provider
 */
public interface DBDRowHintProvider extends DBDValueHintProvider {

    /**
     * Get all hints available for specified row.
     *
     * @param model
     * @param types   requested hint types
     * @param options flags combined from HINT_ constants
     */
    @Nullable
    DBDValueHint[] getRowHints(
        @NotNull DBDResultSetModel model,
        @NotNull DBDValueRow row,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options);
}
