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
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.List;

/**
 * DBSAttributeEnumerable
 */
public interface DBSAttributeEnumerable extends DBSEntityAttribute
{
    /**
     * Gets enumeration values
     * @param session               session
     * @param valuePattern          pattern for enumeration values. If null or empty then returns full enumration set
     * @param maxResults            maximum enumeration values in result set
     * @param calcCount             Calculate value count and order by it (desc). Otherwise just read distinct values in natural order
     * @param formatValues          Use value formatting or return raw values
     * @param caseInsensitiveSearch Use case-insensitive search for {@code valuePattern}
     * @return statement with result set which contains valid enumeration values.
     **/
    @NotNull
    List<DBDLabelValuePair> getValueEnumeration(
        @NotNull DBCSession session,
        @Nullable Object valuePattern,
        int maxResults,
        boolean calcCount,
        boolean formatValues,
        boolean caseInsensitiveSearch)
        throws DBException;

}