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

public abstract class DisposableLazyValue<T, E extends Exception> extends LazyValue<T, E> {

    /**
     * Disposes the cached value. After calling dispose(), evaluate() will reinitialize the value.
     *
     * @throws E if an exception occurs during disposal.
     */
    public synchronized void dispose() throws E {
        T disposedValue = this.value;
        // Clear the cached value and exception to force re-evaluation
        this.value = null;
        this.exception = null;
        onDispose(disposedValue);
    }

    /**
     * Performs cleanup operations when disposing the cached value.
     *
     * @param disposedValue the value that was disposed.
     * @throws E if an exception occurs during disposal.
     */
    protected abstract void onDispose(T disposedValue) throws E;
}
