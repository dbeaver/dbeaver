/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.importer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporterSite;

/**
 * Abstract stream importer
 */
public abstract class StreamImporterAbstract implements IStreamDataImporter {

    private IStreamDataImporterSite site;

    public IStreamDataImporterSite getSite()
    {
        return site;
    }

    @Override
    public void init(@NotNull IStreamDataImporterSite site) throws DBException
    {
        this.site = site;
    }

    @Override
    public void dispose()
    {
        // do nothing
    }

}