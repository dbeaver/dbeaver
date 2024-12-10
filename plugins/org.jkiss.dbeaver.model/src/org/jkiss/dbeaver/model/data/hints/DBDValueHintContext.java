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
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * Value hint context.
 * It is usually a data viewer presentation.
 * Context remains the same for a data container (e.g. a table).
 * Context can be used by hint providers to keep cache related to data container.
 * Every hint provider may get/set any number of custom attributes here
 */
public interface DBDValueHintContext {

    @Nullable
    DBSDataContainer getDataContainer();

    /**
     * Get context attribute value
     */
    @Nullable
    Object getHintContextAttribute(@NotNull String name);

    /**
     * Set context attribute value
     */
    void setHintContextAttribute(@NotNull String name, @Nullable Object value);

}
