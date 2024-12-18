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
package org.jkiss.dbeaver.model.data.hints.standard;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDResultSetModel;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.data.hints.DBDAttributeHintProvider;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.ValueHintText;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Attribute keys hint provider
 */
public class AttributeKeysHintProvider implements DBDAttributeHintProvider {

    @Nullable
    @Override
    public DBDValueHint[] getAttributeHints(
        @NotNull DBDResultSetModel model,
        @NotNull DBDAttributeBinding attribute,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options
    ) {
        List<DBDValueHint> hints = new ArrayList<>();

        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();

        if (rowIdentifier != null &&
            !rowIdentifier.isIncomplete() &&
            rowIdentifier != model.getDefaultRowIdentifier()
        ) {
            hints.add(
                new ValueHintText(
                    "Unique key: " +
                        rowIdentifier.getEntity().getName() +
                        "(" +
                        rowIdentifier.getAttributes().stream().map(DBDAttributeBinding::getName)
                            .collect(Collectors.joining(",")) +
                        ")",
                    "Unique key which will be used to edit this column's value",
                    null));
        }

        if (rowIdentifier != null && rowIdentifier.hasAttribute(attribute)) {
            hints.add(
                new ValueHintText(
                    "Part of key: " +
                        DBUtils.getObjectFullName(rowIdentifier.getUniqueKey(), DBPEvaluationContext.UI),
                    null,
                    DBIcon.OVER_KEY));
        }
        if (!CommonUtils.isEmpty(attribute.getReferrers())) {
            hints.add(
                new ValueHintText(
                    "Refers to: " + getRefTableNames(attribute.getReferrers()),
                    null,
                    DBIcon.OVER_REFERENCE));
        }
        return hints.toArray(new DBDValueHint[0]);
    }

    private String getRefTableNames(List<DBSEntityReferrer> referrers) {
        return referrers.stream()
            .map(r -> {
                if (r instanceof DBSEntityAssociation assoc) {
                    DBSEntity entity = assoc.getAssociatedEntity();
                    if (entity != null) {
                        return DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI)
                        /* + "(" +
                           r.getAttributeReferences(null).stream()
                           .map(ar -> ar.getAttribute().getName())+ ")"*/;
                    }
                }
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));
    }
}
