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
package org.jkiss.dbeaver.model.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Task info collector.
 * May be implemented by {@link DBTTaskHandler}
 */
public interface DBTTaskInfoCollector {

    void collectTaskInfo(@NotNull DBTTask task, @NotNull TaskInformation information);

    class TaskInformation {
        private final Set<DBPDataSourceContainer> dataSources = new LinkedHashSet<>();
        private final Set<String> ioLocations = new LinkedHashSet<>();
        private final Set<String> inetAddresses = new LinkedHashSet<>();

        @NotNull
        public Collection<DBPDataSourceContainer> getDataSources() {
            return dataSources;
        }

        @NotNull
        public Collection<String> getIOLocations() {
            return ioLocations;
        }

        @NotNull
        public Collection<String> getInetAddresses() {
            return inetAddresses;
        }

        public void addDataSource(@Nullable DBPDataSourceContainer ds) {
            if (ds != null) {
                dataSources.add(ds);
            }
        }

        public void addLocation(@Nullable String pathOrURI) {
            if (!CommonUtils.isEmpty(pathOrURI)) {
                ioLocations.add(pathOrURI);
            }
        }

        public void addInetAddress(@Nullable String hostOrIp) {
            if (!CommonUtils.isEmpty(hostOrIp)) {
                inetAddresses.add(hostOrIp);
            }
        }

    }

}
