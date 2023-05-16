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
package org.jkiss.dbeaver.registry.task;

import com.google.gson.Gson;
import org.jkiss.dbeaver.Log;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TaskUtils {
    private static final Log log = Log.getLog(TaskUtils.class);
    static final String RUN_LOG_PREFIX = "run_";
    static final String RUN_LOG_EXT = "log";


    public static List<TaskRunImpl> loadRunStatistics(Path metaFile, Gson gson) {
        if (!Files.exists(metaFile)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(metaFile)) {
            var statistics = gson.fromJson(reader, RunStatistics.class);
            if (statistics == null) {
                log.error("Null task run statistics returned");
                return new ArrayList<>();
            }
            return statistics.getRuns();
        } catch (Exception e) {
            log.error("Error reading task run statistics", e);
            return new ArrayList<>();
        }
    }


    public static String buildRunLogFileName(String runId) {
        return RUN_LOG_PREFIX + runId + "." + RUN_LOG_EXT;
    }
}
