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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Dictionary table (entity).
 * May return possible values for a set of attributes.
 */
public interface DBSDictionary
{
    /**
     * Checks that this constrain supports key enumerations.
     * Usually it depends on constraint type (enumerations makes sense only for unique constraints).
     * @return true or false
     */
    boolean supportsDictionaryEnumeration();

    /**
     * Gets enumeration values
     * @param monitor session
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param sortByValue sort results by value
     * @param sortAsc ascending sorting (irrelevant is @sortByValue is false)
     * @param caseInsensitiveSearch use case-insensitive search for {@code keyPattern}
     * @param maxResults maximum enumeration values in result set
     * @return statement with result set which contains valid enumeration values.
     */
    @NotNull
    List<DBDLabelValuePair> getDictionaryEnumeration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        Object keyPattern,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        boolean caseInsensitiveSearch,
        int maxResults)
        throws DBException;

    @NotNull
    List<DBDLabelValuePair> getDictionaryValues(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        @NotNull List<Object> keyValues,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc)
        throws DBException;

}