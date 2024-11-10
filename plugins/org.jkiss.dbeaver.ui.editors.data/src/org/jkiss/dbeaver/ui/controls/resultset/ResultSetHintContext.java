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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.data.IValueHintContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Result set hint context
 */
class ResultSetHintContext implements IValueHintContext {

    private final Supplier<DBSDataContainer> dataContainerSupplier;
    private final Map<String, Object> attributes = new HashMap<>();

    ResultSetHintContext(Supplier<DBSDataContainer> dataContainerSupplier) {
        this.dataContainerSupplier = dataContainerSupplier;
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainerSupplier.get();
    }

    @Nullable
    @Override
    public Object getHintContextAttribute(@NotNull String name) {
        return attributes.get(name);
    }

    @Override
    public void setHintContextAttribute(@NotNull String name, @Nullable Object value) {
        this.attributes.put(name, value);
    }

    void resetCache() {
        this.attributes.clear();
    }
}
