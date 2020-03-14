/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.model;

/**
 * Exclusive resource is an object which can be used only by a single thread.
 * It is needed to avoid low-level synchronization (which may lead to deadlocks).
 */
public interface DBPExclusiveResource
{
    /**
     * Acquires exclusive resource lock. Waits until resource will be available.
     * @return lock object. Caller MUST call this function in pair with releaseExclusiveLock in try/finally block.
     */
    Object acquireExclusiveLock();

    /**
     * Releases exclusive lock. Threads waiting in acquireExclusiveLock now can continue.
     * @param lock lock object obtained in acquireExclusiveLock.
     */
    void releaseExclusiveLock(Object lock);

}
