/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.process;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.jkiss.code.NotNull;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProcessNameUtils {

    private static final String OS_WINDOWS = "win";
    private static final String OS_LINUX = "linux";
    private static final String OS_MAC = "mac";

    private interface Carbon extends Library {
        Carbon INSTANCE = Native.load("ApplicationServices", Carbon.class);

        int GetCurrentProcess(@NotNull int[] psn);

        int CPSSetProcessName(@NotNull int[] psn, @NotNull byte[] name);
    }

    private ProcessNameUtils() {
    }

    @NotNull
    public static List<String> buildNamedCommand(@NotNull String processName, @NotNull List<String> command) {
        if (isWindows()) {
            return command;
        }

        String shell = isLinux() ? "/bin/bash" : "/bin/sh";
        List<String> shellCommand = new ArrayList<>();
        shellCommand.add(shell);
        shellCommand.add("-c");
        shellCommand.add(buildExecNamedCommand(processName, command));
        return shellCommand;
    }

    public static void setRuntimeProcessName(@NotNull String processName) throws IOException {
        if (isMac()) {
            setMacProcessName(processName);
        } else if (isLinux()) {
            Files.writeString(Path.of("/proc/self/comm"), processName + "\n");
        }
    }

    @NotNull
    private static String buildExecNamedCommand(@NotNull String processName, @NotNull List<String> command) {
        List<String> execCommand = new ArrayList<>();
        execCommand.add("exec");
        execCommand.add("-a");
        execCommand.add(quoteShellArg(processName));
        for (String arg : command) {
            execCommand.add(quoteShellArg(arg));
        }
        return String.join(" ", execCommand);
    }

    @NotNull
    private static String quoteShellArg(@NotNull String arg) {
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    private static void setMacProcessName(@NotNull String processName) {
        int[] psn = new int[2];
        int rc = Carbon.INSTANCE.GetCurrentProcess(psn);
        if (rc != 0) {
            throw new IllegalStateException("GetCurrentProcess rc=" + rc);
        }
        byte[] utf8 = (processName + "\0").getBytes(StandardCharsets.UTF_8);
        rc = Carbon.INSTANCE.CPSSetProcessName(psn, utf8);
        if (rc != 0) {
            throw new IllegalStateException("CPSSetProcessName rc=" + rc);
        }
    }

    private static boolean isWindows() {
        return getOsName().contains(OS_WINDOWS);
    }

    private static boolean isLinux() {
        return getOsName().contains(OS_LINUX);
    }

    private static boolean isMac() {
        return getOsName().contains(OS_MAC);
    }

    @NotNull
    private static String getOsName() {
        return System.getProperty(StandardConstants.ENV_OS_NAME, "").toLowerCase(Locale.ENGLISH);
    }
}
