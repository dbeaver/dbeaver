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

package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;

import java.io.IOException;

/**
 * IStreamDataExporter
 */
public interface IStreamDataExporter extends IDataTransferProcessor {

    void init(IStreamDataExporterSite site)
        throws DBException;

    void exportHeader(DBCSession session)
        throws DBException, IOException;

    void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row)
        throws DBException, IOException;

    void exportFooter(DBRProgressMonitor monitor)
        throws DBException, IOException;

    void dispose();

}
