/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

    public final static Options ALL_OPTIONS = new Options()
        .addOption(PARAM_HELP, false, "Help")

        .addOption(PARAM_FILE, "file", true, "File top open")
        .addOption(PARAM_STOP, "quit", false, "Stop DBeaver running instance")
        .addOption(PARAM_THREAD_DUMP, "thread-dump", false, "Print instance thread dump")
        // Eclipse options
        .addOption("product", true, "Product id")
        .addOption("nl", true, "National locale")
        .addOption("data", true, "Data directory")
        .addOption("nosplash", false, "No splash screen")
        .addOption("showlocation", false, "Show location")
        ;
}
