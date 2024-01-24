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
package org.jkiss.dbeaver.slf4j;

import org.jkiss.dbeaver.Log;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class DefaultLoggerBinder implements ILoggerFactory {

    private static final Log log = Log.getLog(DefaultLoggerBinder.class);

    private static final DefaultLoggerBinder SINGLETON = new DefaultLoggerBinder();

    public static DefaultLoggerBinder getSingleton() {
        return SINGLETON;
    }

    @Override
    public Logger getLogger(String name) {
        return new SLFLogger(name);
    }

}
