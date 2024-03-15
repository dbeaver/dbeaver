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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class BigQueryTrace implements DBCTrace {
    private static final Log log = Log.getLog(BigQueryTrace.class);
    private Map<String, String> resultDetails;
    private final NumberFormat numberFormat = new DecimalFormat();

    public BigQueryTrace(@NotNull BigQueryResultSet resultSet) {
        resultDetails = new LinkedHashMap<>();
        try {
            Object mResultSet = BeanUtils.getInheritedFieldValue(resultSet.getOriginal(), "m_resultSet"); //$NON-NLS-1$
            Object mDataHandler = BeanUtils.getInheritedFieldValue(mResultSet, "m_dataHandler"); //$NON-NLS-1$
            Object mBqResults = BeanUtils.getInheritedFieldValue(mDataHandler, "m_bqResults"); //$NON-NLS-1$
            Object mQueryResponse = BeanUtils.getInheritedFieldValue(mBqResults, "m_queryResponse"); //$NON-NLS-1$
            // QueryResponse
            Object totalBytes = BeanUtils.invokeObjectMethod(mQueryResponse, "getTotalBytesProcessed"); // $NON-NLS-1$
            Object totalRows = BeanUtils.invokeObjectMethod(mQueryResponse, "getTotalRows"); // $NON-NLS-1$
            Object cacheHit = BeanUtils.invokeObjectMethod(mQueryResponse, "getCacheHit"); // $NON-NLS-1$
            Object numDmlAffectedRows = BeanUtils.invokeObjectMethod(mQueryResponse, "getNumDmlAffectedRows"); // $NON-NLS-1$
            // DmlStatistics
            Object dmlStats = BeanUtils.invokeObjectMethod(mQueryResponse, "getDmlStats"); // $NON-NLS-1$
            // JobReference
            Object jobReference = BeanUtils.invokeObjectMethod(mQueryResponse, "getJobReference"); // $NON-NLS-1$
            if (jobReference != null) {
                Object jobId = BeanUtils.invokeObjectMethod(jobReference, "getJobId"); // $NON-NLS-1$
                Object location = BeanUtils.invokeObjectMethod(jobReference, "getLocation"); // $NON-NLS-1$
                Object projectId = BeanUtils.invokeObjectMethod(jobReference, "getProjectId"); // $NON-NLS-1$
                resultDetails.put("Job id", String.valueOf(jobId)); //$NON-NLS-1$
                resultDetails.put("Location", String.valueOf(location)); //$NON-NLS-1$
                resultDetails.put("Project id", String.valueOf(projectId)); //$NON-NLS-1$
            }
            if (dmlStats != null) {
                Object dmlDeletedRowsCount = BeanUtils.invokeObjectMethod(dmlStats, "getDeletedRowCount"); // $NON-NLS-1$
                Object dmlInsertedRowCount = BeanUtils.invokeObjectMethod(dmlStats, "getInsertedRowCount"); // $NON-NLS-1$
                Object dmlUpdatedRowCount = BeanUtils.invokeObjectMethod(dmlStats, "getUpdatedRowCount"); // $NON-NLS-1$
                resultDetails.put("DML deleted", String.valueOf(dmlDeletedRowsCount)); //$NON-NLS-1$
                resultDetails.put("DML inserted", String.valueOf(dmlInsertedRowCount)); //$NON-NLS-1$
                resultDetails.put("DML updated", String.valueOf(dmlUpdatedRowCount)); //$NON-NLS-1$
            }
            resultDetails.put("Fetched from cache", String.valueOf(cacheHit)); //$NON-NLS-1$
            resultDetails.put("DML affected rows", numDmlAffectedRows == null ? "none" : String.valueOf(numDmlAffectedRows)); //$NON-NLS-1$
            resultDetails.put("Total bytes processed", numberFormat.format(totalBytes) + " bytes"); //$NON-NLS-1$
            resultDetails.put("Total rows", String.valueOf(totalRows)); //$NON-NLS-1$
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public Map<String, String> getResultDetails() {
        return resultDetails;
    }

}
