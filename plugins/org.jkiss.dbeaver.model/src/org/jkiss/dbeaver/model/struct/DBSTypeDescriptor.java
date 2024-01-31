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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Provides extra type description, not covered by the database model
 */
public interface DBSTypeDescriptor {
    /**
     * Returns underlying data type, if presented
     */
    @Nullable
    DBSDataType getUnderlyingType();

    /**
     * Returns data type name
     */
    @NotNull
    String getTypeName();

    /**
     * Returns true if described data type represents an indexable collection (like array)
     */
    boolean isIndexable();

    /**
     * Returns the amount of indexable dimensions for indexable data type
     */
    int getIndexableDimensions();

    /**
     * Returns type description for the result of the collection indexing
     * with the given amount of indexes or splicing specification
     */
    @Nullable
    DBSTypeDescriptor getIndexableItemType(int depth, boolean[] slicingSpecOrNull);

}
