/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.sql.task.SQLToolRunStatisticsGenerator;
import org.jkiss.dbeaver.model.sql.task.SQLToolStatistics;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract tool with status
 */
public abstract class MySQLToolWithStatus<OBJECT_TYPE extends DBSObject, SETTINGS extends SQLToolExecuteSettings<OBJECT_TYPE>>
    extends SQLToolExecuteHandler<OBJECT_TYPE, SETTINGS> implements SQLToolRunStatisticsGenerator<OBJECT_TYPE, SETTINGS, DBEPersistAction>
{
    @Override
    public List<ToolStatus> getExecuteStatistics(OBJECT_TYPE object, SETTINGS settings, DBEPersistAction action, DBCSession session, DBCStatement dbStat) throws DBCException {
        DBCResultSet dbResult = dbStat.openResultSet();
        if (!(dbResult instanceof JDBCResultSet)) {
            return Collections.emptyList();
        }
        try {
            List<ToolStatus> statusList = new ArrayList<>();
            while (dbResult.nextRow()) {
                statusList.add(
                    new ToolStatus(
                        object,
                        JDBCUtils.safeGetString((JDBCResultSet) dbResult, "Msg_type"),
                        JDBCUtils.safeGetString((JDBCResultSet) dbResult, "Msg_text")));
            }
            return statusList;
        } finally {
            dbResult.close();
        }
    }

    public class ToolStatus extends SQLToolStatistics<OBJECT_TYPE> {
        private final String messageType;
        private final String messageText;

        ToolStatus(OBJECT_TYPE object, String messageType, String messageText) {
            super(object);
            this.messageType = messageType;
            this.messageText = messageText;
        }

        @Property(viewable = true, order = 1)
        @Override
        public OBJECT_TYPE getObject() {
            return super.getObject();
        }

        @Property(viewable = true, order = 2)
        public String getMessageType() {
            return messageType;
        }

        @Property(viewable = true, order = 3)
        public String getMessageText() {
            return messageText;
        }

    }

}
