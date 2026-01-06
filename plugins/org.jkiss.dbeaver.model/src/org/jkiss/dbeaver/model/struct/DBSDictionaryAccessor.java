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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;


public interface DBSDictionaryAccessor extends AutoCloseable {

    @NotNull
    DBRProgressMonitor getProgressMonitor();

    boolean isKeyComparable();

    @NotNull
    List<DBDLabelValuePair> getSimilarValues(
        @NotNull Object pattern,
        boolean caseInsensitive,
        boolean byDesc,
        long offset,
        long maxResults
    ) throws DBException;

    @NotNull
    List<DBDLabelValuePair> getValuesNear(
        @NotNull Object value,
        boolean isPreceeding,
        long offset,
        long maxResults
    ) throws DBException;

    @NotNull
    List<DBDLabelValuePair> getSimilarValuesNear(
        @NotNull Object pattern,
        boolean caseInsensitive,
        boolean byDesc,
        Object value, boolean isPreceeding,
        long offset,
        long maxResults
    ) throws DBException;
    
    @NotNull
    List<DBDLabelValuePair> getValueEntry(@NotNull Object keyValue) throws DBException;

    @NotNull
    List<DBDLabelValuePair> getValues(long offset, int pageSize) throws DBException;
}

