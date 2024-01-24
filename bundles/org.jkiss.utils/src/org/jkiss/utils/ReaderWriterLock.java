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
package org.jkiss.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class ReaderWriterLock<MUTATOR> {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Supplier<MUTATOR> mutatorSupplier;

    public ReaderWriterLock(Supplier<MUTATOR> mutatorSupplier) {
        this.mutatorSupplier = mutatorSupplier;
    }

    /**
     * Represents an operation that accepts a single input argument and returns no result possibly throwing an exception.
     */
    public interface ExceptableConsumer<T, EXCEPTION extends Throwable> {
        /**
         * Performs the operation on the given argument.
         */
        void accept(T t) throws EXCEPTION;
    }

    /**
     * Represents an operation that accepts a single input argument and produces a result possibly throwing an exception.
     */
    public interface ExceptableFunction<T, RESULT, EXCEPTION extends Throwable> {
        /**
         * Performs the operation on the given argument and returns the result.
         */
        RESULT apply(T t) throws EXCEPTION;
    }

    /**
     * Acquires the reader or writer lock, executes given operation and releases the lock returning the operation result.
     */
    public <RESULT, EXCEPTION extends Throwable> RESULT compute(
        boolean writing,
        ExceptableFunction<MUTATOR, RESULT, EXCEPTION> action
    ) throws EXCEPTION {
        Lock lock = writing ? rwLock.writeLock() : rwLock.readLock();
        lock.lock();
        try {
            return action.apply(mutatorSupplier.get());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquires the reader lock, executes given operation and releases the lock returning the operation result.
     */
    public <RESULT, EXCEPTION extends Throwable> RESULT computeReading(
        ExceptableFunction<MUTATOR, RESULT, EXCEPTION> action
    ) throws EXCEPTION {
        return this.compute(false, action);
    }

    /**
     * Acquires the writer lock, executes given operation and releases the lock returning the operation result.
     */
    public <RESULT, EXCEPTION extends Throwable> RESULT computeWriting(
        ExceptableFunction<MUTATOR, RESULT, EXCEPTION> action
    ) throws EXCEPTION {
        return this.compute(true, action);
    }

    /**
     * Acquires the reader or writer lock, executes given operation and releases the lock.
     */
    public <EXCEPTION extends Throwable> void exec(
        boolean writing,
        ExceptableConsumer<MUTATOR, EXCEPTION> action
    ) throws EXCEPTION {
        this.compute(writing, m -> {
            action.accept(m);
            return null;
        });
    }

    /**
     * Acquires the reader lock, executes given operation and releases the lock.
     */
    public <EXCEPTION extends Throwable> void execReading(ExceptableConsumer<MUTATOR, EXCEPTION> action) throws EXCEPTION {
        this.exec(false, action);
    }

    /**
     * Acquires the writer lock, executes given operation and releases the lock.
     */
    public <EXCEPTION extends Throwable> void execWriting(ExceptableConsumer<MUTATOR, EXCEPTION> action) throws EXCEPTION {
        this.exec(true, action);
    }
}
