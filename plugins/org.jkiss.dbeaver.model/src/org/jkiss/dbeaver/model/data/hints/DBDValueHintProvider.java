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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.EnumSet;

/**
 * Value hint provider
 */
public interface DBDValueHintProvider {

    int OPTION_INLINE = 1;
    int OPTION_ADVANCED = 1 << 1;
    int OPTION_TOOLTIP = 1 << 2;
    int OPTION_ACTION_TOOLTIP = 1 << 3;
    int OPTION_ROW_EXPANDED = 1 << 4;

    /**
     * Get all hints available for specified value.
     *
     * @param types   requested hint types
     * @param options flags combined from HINT_ constants
     */
    @Nullable
    DBDValueHint[] getValueHint(
        @NotNull DBDValueHintContext context,
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDValueRow row,
        @Nullable Object value,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options);

    /**
     * Calculates approximate length (in characters) of hint text
     */
    default int getAttributeHintSize(
        @NotNull DBDValueHintContext context,
        @NotNull DBDAttributeBinding attribute
    ) {
        return 0;
    }

        /**
         * Read all necessary data which is needed to render hints.
         */
    default void cacheRequiredData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBDValueHintContext context,
        @NotNull Collection<DBDAttributeBinding> attributes,
        @NotNull Collection<? extends DBDValueRow> rows,
        boolean cleanupCache
    ) throws DBException {

    }

}
