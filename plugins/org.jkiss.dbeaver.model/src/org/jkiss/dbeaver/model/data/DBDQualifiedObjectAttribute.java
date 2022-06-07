/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.*;


public interface DBDQualifiedObjectAttribute extends DBSAttributeBase, DBSObject, DBPQualifiedObject {

    @Nullable
    @Override
    DBDQualifiedObjectAttribute getParentObject();

    boolean isPseudoAttribute();

    @NotNull
    @Override
    default String getFullyQualifiedName(@Nullable DBPEvaluationContext context) {
        final DBPDataSource dataSource = getDataSource();
        if (getParentObject() == null) {
            return DBUtils.getQuotedIdentifier(dataSource, getName());
        }
        char structSeparator = dataSource.getSQLDialect().getStructSeparator();

        StringBuilder query = new StringBuilder();
        boolean hasPrevIdentifier = false;
        for (DBDQualifiedObjectAttribute attribute = this; attribute != null; attribute = attribute.getParentObject()) {
            if (attribute.isPseudoAttribute() ||
                (attribute.getParentObject() == null && attribute.getDataKind() == DBPDataKind.DOCUMENT)
            ) {
                // Skip pseudo attributes and document attributes (e.g. Mongo root document)
                continue;
            }
            if (hasPrevIdentifier) {
                query.insert(0, structSeparator);
            }
            query.insert(0, DBUtils.getQuotedIdentifier(dataSource, attribute.getName()));
            hasPrevIdentifier = true;
        }

        return query.toString();
    }
}
