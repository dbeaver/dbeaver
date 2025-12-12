/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data.order;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.List;

/**
 * Utilities for applying default ordering in a non-UI layer.
 */
public final class OrderingUtils {
    private static final Log log = Log.getLog(OrderingUtils.class);

    private OrderingUtils() {
        // no instances
    }

    public static boolean addOrderingOnClientSide(
        @NotNull DBDDataFilter dataFilter,
        @Nullable DBDRowIdentifier rowIdentifier,
        @NotNull OrderingPolicy policy
    ) {
        if (policy == OrderingPolicy.DEFAULT) {
            return false;
        }

        if (rowIdentifier == null || rowIdentifier.isIncomplete()) {
            return false;
        }

        for (DBDAttributeBinding binding : rowIdentifier.getAttributes()) {
            DBDAttributeConstraint constraint = dataFilter.getConstraint(binding);
            if (constraint != null) {
                constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
                constraint.setOrderDescending(policy == OrderingPolicy.PRIMARY_KEY_DESC);
            }
        }
        return true;
    }


    public static void addOrderingOnServerSide(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity entity,
        @NotNull DBDDataFilter dataFilter,
        @NotNull OrderingPolicy policy
    ) {
        if (!entity.getDataSource().getSQLDialect().supportsOrderByIndex() || policy == OrderingPolicy.DEFAULT) {
            return;
        }

        List<? extends DBSEntityAttribute> attrs = List.of();
        try {
            attrs = DBUtils.getBestTableIdentifier(monitor, entity);
        } catch (DBException exception) {
            log.warn("Can't get table identifier", exception);
        }

        if (attrs.isEmpty()) {
            return;
        }
        dataFilter.setOrder(String.join(",", attrs.stream()
            .map(attr -> DBUtils.getQuotedIdentifier(entity.getDataSource(), attr.getName()))
            .toList()) + " " + (policy == OrderingPolicy.PRIMARY_KEY_DESC ? "DESC" : "ASC"));
    }
}
