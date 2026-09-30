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
import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;

/**
 * Collects the name of a new Mimer SQL schema before it is created.
 *
 * @author Mimer Information Technology
 */
public class MimerSchemaConfigurator extends AbstractMimerConfigurator<MimerSchema> {

    @NotNull
    @Override
    protected MimerCreatePage createPage(@NotNull MimerSchema schema, @Nullable Object container) {
        return new MimerCreateSchemaPage(schema);
    }
}
