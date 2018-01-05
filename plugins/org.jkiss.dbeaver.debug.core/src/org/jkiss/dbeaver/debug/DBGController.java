/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug;

import java.util.Map;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBRResult;

//FIXME:AF: we need "operation result" interface like IStatus to express the result of operation
//FIXME:AF: so let's return void for now and let's throw an exception for any issue (poor practice)

/**
 * This interface is expected to be used in synch manner
 */
public interface DBGController {
    
    void init(DataSourceDescriptor dataSourceDescriptor, String databaseName, Map<String, Object> attributes);

    public DBRResult connect(DBRProgressMonitor monitor);

    void resume();

    void suspend();

    void terminate();
    
    void dispose();

}
