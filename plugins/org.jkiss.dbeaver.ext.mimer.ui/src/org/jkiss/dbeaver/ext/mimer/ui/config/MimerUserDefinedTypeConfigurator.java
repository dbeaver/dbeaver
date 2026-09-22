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
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Collects all attributes of a new Mimer SQL user-defined type before it is created - picks
 * {@link MimerCreateDistinctTypePage} or {@link MimerCreateStructuredTypePage} by {@link
 * MimerUserDefinedType#getCategory()}, already pre-seeded by {@code
 * MimerUserDefinedTypeManager#detectCategory} from whichever folder was clicked.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedTypeConfigurator implements DBEObjectConfigurator<MimerUserDefinedType> {

    @Nullable
    @Override
    public MimerUserDefinedType configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull MimerUserDefinedType type,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            boolean structured = "STRUCTURED".equalsIgnoreCase(type.getCategory());
            if (structured) {
                MimerCreateStructuredTypePage page = new MimerCreateStructuredTypePage(type);
                if (!page.edit()) {
                    return null;
                }
                page.applyChanges();
            } else {
                MimerCreateDistinctTypePage page = new MimerCreateDistinctTypePage(type);
                if (!page.edit()) {
                    return null;
                }
                page.applyChanges();
            }
            return type;
        });
    }
}
