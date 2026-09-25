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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankFile;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Collects the attributes of a new Mimer SQL databank file before it is added.
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankFileConfigurator implements DBEObjectConfigurator<MimerDatabankFile> {

    @Nullable
    @Override
    public MimerDatabankFile configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull MimerDatabankFile file,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            MimerCreateDatabankFilePage page = new MimerCreateDatabankFilePage(file);
            if (!page.edit()) {
                return null;
            }
            page.applyChanges();
            return file;
        });
    }
}
