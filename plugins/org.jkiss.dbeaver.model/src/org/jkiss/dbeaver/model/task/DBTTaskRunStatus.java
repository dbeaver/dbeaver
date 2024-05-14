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
package org.jkiss.dbeaver.model.task;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.messages.ModelMessages;

import java.util.StringJoiner;

public class DBTTaskRunStatus {

    private String resultMessage;

    public DBTTaskRunStatus(){
    }

    @Nullable
    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String message) {
        resultMessage = message;
    }

    public static DBTTaskRunStatus makeStatisticsStatus(DBCStatistics statistics) {
        DBTTaskRunStatus taskResultStatus = new DBTTaskRunStatus();

        if (statistics.getRowsFetched() > 0 ||
            statistics.getRowsUpdated() > 0 ||
            statistics.getStatementsCount() > 0) {

            StringJoiner joiner = new StringJoiner(", ");
            if (statistics.getRowsFetched() > 0) {
                joiner.add(NLS.bind(ModelMessages.task_rows_fetched_message_part, statistics.getRowsFetched()));
            }
            if (statistics.getRowsUpdated() > 0) {
                joiner.add(NLS.bind(ModelMessages.task_rows_modified_message_part, statistics.getRowsUpdated()));
            }
            if (statistics.getStatementsCount() > 0) {
                joiner.add(NLS.bind(ModelMessages.task_statements_executed_message_part, statistics.getStatementsCount()));
            }
            taskResultStatus.setResultMessage(joiner.toString());
        }

        return taskResultStatus;
    }
}
