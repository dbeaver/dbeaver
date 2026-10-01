/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;

import java.util.List;

public class PreviewConsumer extends DatabaseTransferConsumer {

    private final DBRProgressMonitor ctlMonitor;

    public PreviewConsumer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseMappingContainer mappingContainer
    ) {
        super(mappingContainer.getTarget());
        ctlMonitor = monitor;
        setPreview(true);
    }

    @NotNull
    public DBRProgressMonitor getCtlMonitor() {
        return ctlMonitor;
    }

    @NotNull
    public List<Object[]> getRows() {
        return getPreviewRows();
    }

}
