/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Object which defines the default collation for the attributes it contains (usually a database).
 * Presence of this interface means the database supports column collations at all.
 */
public interface DBSCollationProvider extends DBPObject {

    @Nullable
    String getDefaultCollation();

    boolean isCollatableType(@NotNull String typeName);

    /**
     * False only when the collation is known to be missing on the server.
     * Returns true if the list of supported collations cannot be read.
     */
    boolean isCollationSupported(@NotNull DBRProgressMonitor monitor, @NotNull String collationName);

    /**
     * Collations which may replace the given one, usually the same language with different
     * sensitivity options. Full server lists hold thousands of entries and are useless in a dropdown.
     */
    @NotNull
    List<String> getRelatedCollations(@NotNull DBRProgressMonitor monitor, @Nullable String collationName);

}
