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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

/**
 * Represents an exporter that supports appending to an existing file.
 */
public interface IAppendableDataExporter extends IStreamDataExporter {

    /**
     * Imports data from an existing file that is determined from the given {@code site}.
     * <p>
     * This method is called before the {@link #init(IStreamDataExporterSite)} method
     * because it may affect the way the exporter is initialized.
     *
     * @param site the exporter site
     * @throws DBException on any error
     */
    void importData(@NotNull IStreamDataExporterSite site) throws DBException;

    /**
     * Determines whether the output file should be truncated before writing or not.
     * <p>
     * This method is useful for file formats with strict structure, such as binary formats and etc.
     *
     * @return whether the output file should be truncated before writing or not
     */
    boolean shouldTruncateOutputFileBeforeExport();
}
