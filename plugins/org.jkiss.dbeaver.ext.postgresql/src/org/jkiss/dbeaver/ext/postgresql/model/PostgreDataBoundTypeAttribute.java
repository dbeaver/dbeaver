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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPAttributeReferencePurpose;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeEnumerable;
import org.jkiss.dbeaver.model.struct.DBSContextBoundAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.LinkedList;

/**
 * Provides information about context for data type attribute
 */
public class PostgreDataBoundTypeAttribute<CONTAINER extends DBSEntity & PostgreObject> extends PostgreAttribute<CONTAINER>
    implements DBSEntityAttribute, DBSAttributeEnumerable, DBSContextBoundAttribute {

    private final DBDAttributeBinding context;
    private final PostgreDataTypeAttribute member;
    
    public PostgreDataBoundTypeAttribute(
        @NotNull DBRProgressMonitor monitor,
        @NotNull CONTAINER container,
        @Nullable DBDAttributeBinding context,
        @NotNull PostgreDataTypeAttribute attr
    ) throws DBException {
        super(monitor, container, attr);
        this.context = context;
        this.member = attr;
    }

    @NotNull
    @Override
    public String formatMemberReference(
        boolean isIncludeContainerName,
        @Nullable String containerAliasOrNull,
        @NotNull DBPAttributeReferencePurpose purpose
    ) {
        LinkedList<String> parts = new LinkedList<>();
        parts.addFirst(DBUtils.getQuotedIdentifier(this.member));
        DBDAttributeBinding bindingContext = this.context;
        DBSEntityAttribute entityContext = bindingContext.getEntityAttribute();
        while (entityContext instanceof PostgreDataBoundTypeAttribute boundAttr) {
            parts.addFirst(DBUtils.getQuotedIdentifier(boundAttr.member));
            bindingContext = boundAttr.context;
            entityContext = bindingContext.getEntityAttribute();
        }
        if (entityContext != null) {
            parts.addFirst(DBUtils.getQuotedIdentifier(entityContext));
            if (isIncludeContainerName) {
                if (containerAliasOrNull == null) {
                    if (entityContext.getParentObject() != this.getTable()) {
                        parts.addFirst(DBUtils.getQuotedIdentifier(entityContext.getParentObject()));
                    }
                    parts.addFirst(DBUtils.getObjectFullName(this.getTable(), DBPEvaluationContext.DML));
                } else {
                    parts.addFirst(containerAliasOrNull);
                }
            }
        } else {
            if (isIncludeContainerName) {
                parts.addFirst(DBUtils.getQuotedIdentifier(bindingContext));
                if (containerAliasOrNull != null) {
                    parts.addFirst(containerAliasOrNull);
                }
            }
        }
        if (purpose.equals(DBPAttributeReferencePurpose.DATA_SELECTION)) {
            return "(".repeat(parts.size() - 1) + String.join(").", parts);
        } else {
            return String.join(".", parts);
        }
    }

    @NotNull
    @Override
    public PostgreSchema getSchema() {
        return member.getSchema();
    }
}
