/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ApplicationCommandLine<T extends ApplicationInstanceController> {
    private static final Log log = Log.getLog(ApplicationCommandLine.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.commandLine";

    public static final String PARAM_HELP = "help";
    public static final String PARAM_THREAD_DUMP = "dump";
    public static final String PARAM_DB_LIST = "databaseList";
    private static final String PARAM_VERSION = "version";

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, PARAM_HELP, false, "Help")
        .addOption(PARAM_DB_LIST, "database-driver-list", true, "Show list of supported database drivers in json format")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        .addOption(
            PARAM_VERSION,
            PARAM_VERSION,
            false,
            "Displays the app name, edition, and version in Major.Minor.Micro.Timestamp format"
        );

    protected ApplicationCommandLine() {
    }

    public CmdProcessResult executeCommandLineCommands(
        @Nullable CommandLine commandLine,
        @Nullable T controller,
        boolean uiActivated
    ) throws Exception {
        if (commandLine == null || (ArrayUtils.isEmpty(commandLine.getArgs()) && ArrayUtils.isEmpty(commandLine.getOptions()))) {
            return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
        }

        if (controller == null) {
            log.debug("Can't process commands because no running instance is present");
            return new CmdProcessResult(CmdProcessResult.PostAction.START_INSTANCE);
        }

        if (commandLine.hasOption(PARAM_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.setWidth(120);
            helpFormatter.setOptionComparator((o1, o2) -> 0);
            helpFormatter.printHelp("dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, "(C) 2010-2025 DBeaver Corp", true);
            try (
                var out = new StringWriter();
                var print = new PrintWriter(out)
            ) {
                helpFormatter.printHelp(
                    print, 100, "dbeaver", GeneralUtils.getProductTitle(), ALL_OPTIONS, 4, 4, "(C) 2010-2025 DBeaver Corp", true
                );
                return new CmdProcessResult(CmdProcessResult.PostAction.SHUTDOWN, out.toString());
            } catch (Exception e) {
                log.error("Error handling command line: " + e.getMessage());
                return new CmdProcessResult(CmdProcessResult.PostAction.ERROR, e.getMessage());
            }
        }

        if (!uiActivated) {
            if (commandLine.hasOption(PARAM_THREAD_DUMP)) {
                String threadDump = controller.getThreadDump();
                System.out.println(threadDump);
                return new CmdProcessResult(CmdProcessResult.PostAction.SHUTDOWN, threadDump);
            }

        }
        if (commandLine.hasOption(PARAM_VERSION)) {
            String version = GeneralUtils.getLongProductTitle();
            System.out.println(version);
            return new CmdProcessResult(CmdProcessResult.PostAction.SHUTDOWN, version);
        }

        return new CmdProcessResult(CmdProcessResult.PostAction.UNKNOWN_COMMAND);
    }

    @Nullable
    public CommandLine getCommandLine() {
        return getCommandLine(Platform.getApplicationArgs());
    }

    @Nullable
    public CommandLine getCommandLine(@NotNull String[] args) {
        try {
            // Remove keyring parameter because its name contains special characters
            // Actual valuation of keyring happens in app launcher

            List<String> applicationArgs = Arrays.stream(args).collect(Collectors.toList());
            Iterator<String> iterator = applicationArgs.iterator();
            boolean removeArgs = false;
            while (iterator.hasNext()) {
                String arg = iterator.next();
                if (CommonUtils.isEmpty(arg)) {
                    continue;
                }
                // argument name start with '-', example '-help'
                if (arg.startsWith("-")) {
                    boolean argSupported = ALL_OPTIONS.hasOption(arg);
                    if (argSupported) {
                        removeArgs = false;
                    } else {
                        //remove not supported argument to avoid parser exception
                        //also remove all arguments for this arg
                        iterator.remove();
                        removeArgs = true;
                    }
                } else if (removeArgs) {
                    iterator.remove();
                }
            }

            return new DefaultParser().parse(ALL_OPTIONS, applicationArgs.toArray(new String[0]), false);
        } catch (Exception e) {
            log.warn("Error parsing command line: " + e.getMessage());
            return null;
        }
    }
}
