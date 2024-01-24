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

package org.jkiss.dbeaver.ui.app.standalone.rpc;

import org.jkiss.code.NotNull;
import org.jkiss.utils.rest.RequestMapping;
import org.jkiss.utils.rest.RequestParameter;

import java.util.Map;

/**
 * DBeaver instance controller.
 */
public interface IInstanceController {

    String CONFIG_PROP_FILE = "dbeaver-instance.properties";

    @RequestMapping(value = "ping", timeout = 5)
    long ping(@RequestParameter("payload") long payload);

    @RequestMapping("version")
    String getVersion();

    @RequestMapping("threadDump")
    String getThreadDump();

    @RequestMapping("openFiles")
    void openExternalFiles(
        @RequestParameter("files") @NotNull String[] fileNames);

    @RequestMapping("openConnection")
    void openDatabaseConnection(
        @RequestParameter("spec") @NotNull String connectionSpec);

    @RequestMapping("quit")
    void quit();

    @RequestMapping("closeEditors")
    void closeAllEditors();

    @RequestMapping("executeCommand")
    void executeWorkbenchCommand(
        @RequestParameter("id") @NotNull String commandID);

    @RequestMapping("fireEvent")
    void fireGlobalEvent(
        @RequestParameter("id") @NotNull String eventId,
        @RequestParameter("properties") @NotNull Map<String, Object> properties);

    @RequestMapping("bringToFront")
    void bringToFront();
}