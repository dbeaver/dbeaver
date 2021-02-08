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
package org.jkiss.dbeaver.model.sql.task;

import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * SQLToolRunStatisticsGenerator
 */
public interface SQLToolRunStatisticsGenerator<
    OBJECT_TYPE extends DBSObject,
    SETTINGS extends SQLToolExecuteSettings<OBJECT_TYPE>,
    PERSIST_ACTION extends DBEPersistAction> {

    List<? extends SQLToolStatistics<OBJECT_TYPE>> getExecuteStatistics(OBJECT_TYPE object, SETTINGS settings, PERSIST_ACTION action, DBCSession session, DBCStatement dbStat) throws DBCException;

}
