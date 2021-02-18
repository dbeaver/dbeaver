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
package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.sql.SQLQuery;

/**
 * SQLResultsConsumer
 *
 * @author Serge Rider
 */
public interface SQLResultsConsumer
{
    /**
     * Gets (or opens new) data receiver.
     * @param statement executing statement or null
     * @param resultSetNumber result set number
     * @return
     */
    @Nullable
    DBDDataReceiver getDataReceiver(SQLQuery statement, int resultSetNumber);

}
