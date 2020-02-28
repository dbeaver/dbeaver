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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * Result set attribute meta data
 */
public interface DBCAttributeMetaData extends DBSAttributeBase
{
    /**
     * Source of this metadata object
     */
    @Nullable
    Object getSource();
    /**
     * Attribute label in result set
     * @return label
     */
    @NotNull
    String getLabel();

    /**
     * Owner entity name
     * @return entity name
     */
    @Nullable
    String getEntityName();

    /**
     * Read-only flag
     * @return read-only attribute state
     */
    boolean isReadOnly();

    /**
     * Owner table metadata
     * @return table metadata
     */
    @Nullable
    DBCEntityMetaData getEntityMetaData();

}
