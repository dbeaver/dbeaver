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

package org.jkiss.dbeaver.tools.transfer.serialize;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SerializerContext
 */
public class SerializerContext {

    private final List<Throwable> errors = new ArrayList<>();
    private final Set<String> failedDataSources = new HashSet<>();

    public List<Throwable> getErrors() {
        return errors;
    }

    public void addError(Throwable error) {
        errors.add(error);
    }

    public List<Throwable> resetErrors() {
        List<Throwable> res = new ArrayList<>(this.errors);
        this.errors.clear();
        return res;
    }

    public boolean isDataSourceFailed(DBPDataSourceContainer dataSourceContainer) {
        return failedDataSources.contains(dataSourceContainer.getId());
    }

    public void addDataSourceFail(DBPDataSourceContainer dataSourceContainer) {
        failedDataSources.add(dataSourceContainer.getId());
    }

}
