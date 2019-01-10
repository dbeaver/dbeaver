/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.runtime.load;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class DatabaseLoadService<RESULT> extends AbstractLoadService<RESULT> {

    private DBPDataSource dataSource;

    protected DatabaseLoadService(String serviceName, DBCExecutionContext context) {
        super(serviceName);
        this.dataSource = context.getDataSource();
    }

    protected DatabaseLoadService(String serviceName, DBPDataSource dataSource) {
        super(serviceName);
        this.dataSource = dataSource;
    }

    protected DatabaseLoadService(String serviceName, DBSWrapper wrapper) {
        this(serviceName, wrapper == null || wrapper.getObject() == null ? null : wrapper.getObject().getDataSource());
    }

    @Override
    public Object getFamily() {
        return dataSource;
    }
}