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
package org.jkiss.dbeaver.ext.bigquery.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.bigquery.model.BigQueryResultSet;
import org.jkiss.dbeaver.model.exec.trace.DBCTrace;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.ByteNumberFormat;

import java.util.HashMap;
import java.util.Map;

public class BigQueryTrace implements DBCTrace {
    private static final Log log = Log.getLog(BigQueryTrace.class);
    private Map<String, String> resultDetails;
    private final ByteNumberFormat byteFormater = new ByteNumberFormat();

    public BigQueryTrace(@NotNull BigQueryResultSet resultSet) {
        resultDetails = new HashMap<>();
        try {
            Object mResultSet = BeanUtils.getInheritedFieldValue(resultSet.getOriginal(), "m_resultSet"); //$NON-NLS-1$
            Object mDataHandler = BeanUtils.getInheritedFieldValue(mResultSet, "m_dataHandler"); //$NON-NLS-1$
            Object mBqResults = BeanUtils.getInheritedFieldValue(mDataHandler, "m_bqResults"); //$NON-NLS-1$
            Object mQueryResponse = BeanUtils.getInheritedFieldValue(mBqResults, "m_queryResponse"); //$NON-NLS-1$
            Object totalBytes = BeanUtils.invokeObjectDeclaredMethod(mQueryResponse,
                "getTotalBytesProcessed", // $NON-NLS-1$
                new Class[0],
                new Object[0]);
            Object totalRows = BeanUtils.invokeObjectDeclaredMethod(mQueryResponse,
                "getTotalRows", // $NON-NLS-1$
                new Class[0],
                new Object[0]);
            Object cacheHit = BeanUtils.invokeObjectDeclaredMethod(mQueryResponse,
                "getCacheHit", // $NON-NLS-1$
                new Class[0], new Object[0]);
            resultDetails.put("Total bytes processed", byteFormater.format(totalBytes)); //$NON-NLS-1$
            resultDetails.put("Total rows", String.valueOf(totalRows)); //$NON-NLS-1$
            resultDetails.put("Fetched from cache", String.valueOf(cacheHit)); //$NON-NLS-1$
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public Map<String, String> getResultDetails() {
        return resultDetails;
    }

}
