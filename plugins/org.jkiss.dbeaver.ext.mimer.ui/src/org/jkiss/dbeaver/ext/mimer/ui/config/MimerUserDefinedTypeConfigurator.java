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

/**
 * Collects all attributes of a new Mimer SQL user-defined type before it is created - picks
 * {@link MimerCreateDistinctTypePage} or {@link MimerCreateStructuredTypePage} by {@link
 * MimerUserDefinedType#getCategory()}, already pre-seeded by {@code
 * MimerUserDefinedTypeManager#detectCategory} from whichever folder was clicked.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedTypeConfigurator extends AbstractMimerConfigurator<MimerUserDefinedType> {

    @NotNull
    @Override
    protected MimerCreatePage createPage(@NotNull MimerUserDefinedType type, @Nullable Object container) {
        if ("STRUCTURED".equalsIgnoreCase(type.getCategory())) {
            return new MimerCreateStructuredTypePage(type);
        }
        return new MimerCreateDistinctTypePage(type);
    }
}
