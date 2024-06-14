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
package org.jkiss.dbeaver.runtime;

import org.jkiss.code.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadExceptionsContext {

    private static final Map<Thread, List<Exception>> THREAD_TO_EXCEPTIONS = new ConcurrentHashMap<>();
    private static final Map<Thread, Thread> THREAD_BINDING_MAP = new ConcurrentHashMap<>();
    //private static final Logger log = LoggerFactory.getLogger(ThreadExceptionsContext.class);

    public static List<Exception> getListExceptionsForCurrentThread() {
        Thread threadForRegistry = getThreadForRegistry();
        List<Exception> exceptionList = THREAD_TO_EXCEPTIONS.get(threadForRegistry);
        if(exceptionList == null){
            return new ArrayList<>();
        }
        return new ArrayList<>(exceptionList);
    }

    public static Set<Exception> getSetExceptionsForCurrentThread() {
        return new HashSet<>(getListExceptionsForCurrentThread());
    }

    public static void registerExceptionForCurrentThread(Exception exception) {

        Thread threadForRegistry = getThreadForRegistry();
        List<Exception> exceptionList = THREAD_TO_EXCEPTIONS.get(threadForRegistry);
        if (exceptionList == null) {
            exceptionList = new ArrayList<>();
            exceptionList.add(exception);
            THREAD_TO_EXCEPTIONS.put(threadForRegistry, exceptionList);
            return;
        }
        exceptionList.add(exception);
    }

    /**
     * If the thread which you are registering is a different thread that gonna read exception list
     *
     * @param callerThread a key where you will be able to find exceptions for current thread
     */
    public static void bindCurrentContextFor(Thread callerThread) {
        Thread currentThread = Thread.currentThread();
        //todo if it will need, there can be used a set for threads
        if (THREAD_BINDING_MAP.get(currentThread) != null) {
            //log.warn(String.format("Thread %s is already in the binding map for ThreadExceptionContext. Overriding...",
            //    currentThread.getName()));
        }
        THREAD_BINDING_MAP.put(currentThread, callerThread);
    }


    public static void realiseResources() {

        Thread currentThread = Thread.currentThread();
        while (THREAD_BINDING_MAP.values().remove(currentThread));
        while (THREAD_TO_EXCEPTIONS.keySet().remove(currentThread));
    }

    private static @NotNull Thread getThreadForRegistry() {
        Thread currentThread = Thread.currentThread();
        Thread bindedThread = THREAD_BINDING_MAP.get(currentThread);
        return bindedThread == null ? currentThread : bindedThread;
    }

}
