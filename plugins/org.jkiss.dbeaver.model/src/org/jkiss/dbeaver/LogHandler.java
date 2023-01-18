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
package org.jkiss.dbeaver;

/**
 * Log handler
 */
public interface LogHandler {

    String getName(String name);

    boolean isDebugEnabled(String name);

    boolean isErrorEnabled(String name);

    boolean isFatalEnabled(String name);

    boolean isInfoEnabled(String name);

    boolean isTraceEnabled(String name);

    boolean isWarnEnabled(String name);

    void trace(String name, Object message);

    void trace(String name, Object message, Throwable t);

    void debug(String name, Object message);

    void debug(String name, Object message, Throwable t);

    void info(String name, Object message);

    void info(String name, Object message, Throwable t);

    void warn(String name, Object message);

    void warn(String name, Object message, Throwable t);

    void error(String name, Object message);

    void error(String name, Object message, Throwable t);

    void fatal(String name, Object message);

    void fatal(String name, Object message, Throwable t);

}
