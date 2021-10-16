/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * Result set table metadata
 */
public interface DBCEntityMetaData {

    @Nullable
    String getCatalogName();

    @Nullable
    String getSchemaName();

    /**
     * Entity name
     */
    @NotNull
    String getEntityName();

    /**
     * Meta attributes which belongs to this entity
     */
    @NotNull
    List<? extends DBCAttributeMetaData> getAttributes();

    default int getCompleteScore() {
        int score = 0;
        if (!CommonUtils.isEmptyTrimmed(this.getCatalogName())) {
            score++;
        }
        if (!CommonUtils.isEmptyTrimmed(this.getSchemaName())) {
            score++;
        }
        if (!CommonUtils.isEmptyTrimmed(this.getEntityName())) {
            score++;
        }
        return score;
    }
}
