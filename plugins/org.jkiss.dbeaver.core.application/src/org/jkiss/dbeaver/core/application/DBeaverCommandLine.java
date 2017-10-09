/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core.application;

import org.apache.commons.cli.Options;

/**
 * command line options
 */
public class DBeaverCommandLine
{
    public static final String PARAM_HELP = "help";
    public static final String PARAM_FILE = "f";
    public static final String PARAM_STOP = "stop";
    public static final String PARAM_THREAD_DUMP = "dump";
    public static final String PARAM_CONNECT = "con";

    public static final String PARAM_CLOSE_TABS = "closeTabs";
    public static final String PARAM_DISCONNECT_ALL = "disconnectAll";

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, false, "Help")

        .addOption(PARAM_FILE, "file", true, "File top open")
        .addOption(PARAM_STOP, "quit", false, "Stop DBeaver running instance")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        .addOption(PARAM_CONNECT, "connect", true, "Connects to a specified database")
        .addOption(PARAM_DISCONNECT_ALL, "disconnectAll", false, "Disconnect from all databases")
        .addOption(PARAM_CLOSE_TABS, "closeTabs", false, "Close all open editors")

        // Eclipse options
        .addOption("product", true, "Product id")
        .addOption("nl", true, "National locale")
        .addOption("data", true, "Data directory")
        .addOption("nosplash", false, "No splash screen")
        .addOption("showlocation", false, "Show location")
        ;
}
