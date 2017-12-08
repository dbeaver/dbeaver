/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql.format;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * SQL Formatter registry
 */
public interface SQLFormatterRegistry {

    /**
     * Creates new formatter. Uses default config and doesn't interact with user for any configuration.
     */
    @Nullable
    SQLFormatter createFormatter(@NotNull SQLFormatterConfiguration configuration);

    /**
     * Creates and configures new formatter. Interacts with user in needed.
     */
    @Nullable
    SQLFormatter createAndConfigureFormatter(@NotNull SQLFormatterConfiguration configuration);

}
