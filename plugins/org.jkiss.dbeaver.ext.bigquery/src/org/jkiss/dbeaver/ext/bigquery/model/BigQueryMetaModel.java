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
package org.jkiss.dbeaver.ext.bigquery.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;

/**
 * Snowflake constants
 */
public class BigQueryMetaModel extends GenericMetaModel implements DBCQueryTransformProvider {
    public BigQueryMetaModel() {
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false);
        }
        return null;
    }

}
