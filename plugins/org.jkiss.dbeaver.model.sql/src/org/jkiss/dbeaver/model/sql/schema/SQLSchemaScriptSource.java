/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.schema;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.Reader;

/**
 * SQL schema manager.
 * Upgrades schema version if needed.
 * Converts schema create/update scripts into target database dialect.
 */
public interface SQLSchemaScriptSource {

    @NotNull
    Reader openSchemaCreateScript(DBRProgressMonitor monitor) throws IOException, DBException;

    @Nullable
    Reader openSchemaUpdateScript(DBRProgressMonitor monitor, int versionNumber) throws IOException, DBException;

}
