/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.engine;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.registry.AISettingsEventListener;
import org.jkiss.dbeaver.model.ai.registry.AISettingsRegistry;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AIEngineFactory<T extends AIEngine> implements AISettingsEventListener {
    private static final Log log = Log.getLog(AIEngineFactory.class);

    protected final @NotNull AISettingsRegistry registry;
    private final AtomicReference<T> engine = new AtomicReference<>();

    public AIEngineFactory(@NotNull AISettingsRegistry registry) {
        this.registry = registry;
        this.registry.addChangedListener(this);
    }

    public T getEngine() throws DBException {
        return engine.updateAndGet(currentEngine -> {
            if (currentEngine == null) {
                try {
                    return createEngine();
                } catch (DBException e) {
                    throw new RuntimeException("Failed to create AI engine", e);
                }
            }
            return currentEngine;
        });
    }

    @NotNull
    protected abstract T createEngine() throws DBException;

    @Override
    public void onSettingsUpdate(@NotNull AISettingsRegistry registry) {
        T andSet = engine.getAndSet(null);
        if (andSet != null) {
            try {
                andSet.close();
            } catch (DBException e) {
                log.error("Failed to close AI engine", e);
            }
        }
    }
}
