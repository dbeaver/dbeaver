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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Script progress listener
 */
public interface SQLScriptProgressListener<T extends DBSObject> {

    void beginScriptProcessing(Job job, Collection<T> objects);

    void endScriptProcessing();

    void beginObjectProcessing(@NotNull T object, int objectNumber);

    void endObjectProcessing(@NotNull T object, Exception error);

    void processObjectResults(@NotNull T object, @Nullable DBCStatement statement, @Nullable DBCResultSet resultSet) throws DBCException;

}
