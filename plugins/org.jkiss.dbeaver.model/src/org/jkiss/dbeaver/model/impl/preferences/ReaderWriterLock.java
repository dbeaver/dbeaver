/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.preferences;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ReaderWriterLock<TMutator> {

    public interface ExceptableConsumer<T, TEx extends Throwable> {
        void accept(T t) throws TEx;
    }

    public interface ExceptableFunction<T, TResult, TEx extends Throwable> {
        TResult apply(T t) throws TEx;
    }

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Supplier<TMutator> mutatorSupplier;
    
    public ReaderWriterLock(Supplier<TMutator> mutatorSupplier) {
        this.mutatorSupplier = mutatorSupplier;
    }

    public <TResult, TEx extends Throwable> TResult compute(boolean writing, ExceptableFunction<TMutator, TResult, TEx> action) throws TEx {
        Lock lock = writing ? rwLock.writeLock() : rwLock.readLock();
        lock.lock();
        try {
            return action.apply(mutatorSupplier.get());
        } finally {
            lock.unlock();
        }
    }
    
    public <TResult, TEx extends Throwable> TResult computeReading(ExceptableFunction<TMutator, TResult, TEx> action) throws TEx {
        return this.compute(false, action);
    } 
    
    public <TResult, TEx extends Throwable> TResult computeWriting(ExceptableFunction<TMutator, TResult, TEx> action) throws TEx {
        return this.compute(true, action);
    }

    public <TEx extends Throwable> void exec(boolean writing, ExceptableConsumer<TMutator, TEx> action) throws TEx {
        this.compute(writing, m -> { action.accept(m); return null; });
    }
    
    public <TEx extends Throwable> void execReading(ExceptableConsumer<TMutator, TEx> action) throws TEx {
        this.exec(false, action);
    } 
    
    public <TEx extends Throwable> void execWriting(ExceptableConsumer<TMutator, TEx> action) throws TEx {
        this.exec(true, action);
    }
}
