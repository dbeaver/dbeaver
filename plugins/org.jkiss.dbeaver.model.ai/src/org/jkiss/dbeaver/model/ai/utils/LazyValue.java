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
package org.jkiss.dbeaver.model.ai.utils;

public abstract class LazyValue<T, E extends Exception> {
    protected T value;
    protected E exception;

    /**
     * Evaluates the value. If the value has not been initialized yet, it is initialized by calling {@link #initialize()}.
     *
     * @return the value
     * @throws E if an exception occurred during initialization.
     */
    public synchronized T evaluate() throws E {
        if (value == null) {
            try {
                value = initialize();
                exception = null;
            } catch (Exception e) {
                exception = (E) e;
            }
        }
        if (exception != null) {
            throw exception;
        }
        return value;
    }

    /**
     * Initializes the value.
     *
     * @return the initialized value.
     * @throws E if an exception occurred during initialization.
     */
    protected abstract T initialize() throws E;
}
