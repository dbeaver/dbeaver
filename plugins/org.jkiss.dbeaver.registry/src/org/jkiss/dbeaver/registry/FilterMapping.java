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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Filter mapping
 */
public class FilterMapping {
    public final String typeName;
    DBSObjectFilter defaultFilter;
    Map<String, DBSObjectFilter> customFilters = new HashMap<>();

    FilterMapping(String typeName) {
        this.typeName = typeName;
    }

    // Copy constructor
    FilterMapping(FilterMapping mapping) {
        this.typeName = mapping.typeName;
        this.defaultFilter = mapping.defaultFilter == null ? null : new DBSObjectFilter(mapping.defaultFilter);
        for (Map.Entry<String, DBSObjectFilter> entry : mapping.customFilters.entrySet()) {
            this.customFilters.put(entry.getKey(), new DBSObjectFilter(entry.getValue()));
        }
    }

    @Nullable
    DBSObjectFilter getFilter(@Nullable DBSObject parentObject, boolean firstMatch) {
        if (parentObject == null) {
            return defaultFilter;
        }
        if (!customFilters.isEmpty()) {
            String objectID = getFilterContainerUniqueID(parentObject);
            DBSObjectFilter filter = customFilters.get(objectID);
            if ((filter != null && !filter.isNotApplicable()) || firstMatch) {
                return filter;
            }
        }

        return firstMatch ? null : defaultFilter;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FilterMapping)) {
            return false;
        }
        FilterMapping source = (FilterMapping) obj;
        return
                CommonUtils.equalObjects(typeName, source.typeName) &&
                        CommonUtils.equalObjects(defaultFilter, source.defaultFilter) &&
                        CommonUtils.equalObjects(customFilters, source.customFilters);
    }

    @Override
    public int hashCode() {
        return
            CommonUtils.hashCode(typeName) +
            CommonUtils.hashCode(defaultFilter) +
            CommonUtils.hashCode(customFilters);
    }

    public static String getFilterContainerUniqueID(@Nullable DBSObject parentObject) {
        String objectFullName = DBUtils.getObjectFullName(parentObject, DBPEvaluationContext.UI);
        DBSInstance ownerInstance = DBUtils.getObjectOwnerInstance(parentObject);
        if (!CommonUtils.equalObjects(ownerInstance.getName(), parentObject.getDataSource().getName())) {
            return ownerInstance.getName() + ":" + objectFullName;
        } else {
            return objectFullName;
        }
    }

}
