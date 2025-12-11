/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp
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
package org.jkiss.dbeaver.model.lsp.standalone;


import org.apache.commons.cli.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.lsp.DBLFacade;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone LSP application.
 *
 * @implNote This class is public so OSGi can instantiate it.
 */
public final class LSApplication extends BaseApplicationImpl {

    public static void main(String[] args) throws Exception {
        String[] equinoxArgs = new String[] {
            "-application", LSApplication.class.getName()
        };
        BundleContext startup = EclipseStarter.startup(equinoxArgs, null);

        HashMap<String, Object> contextArgs = new HashMap<>();
        contextArgs.put("application.args", args);

        LSApplication instance = (LSApplication) getInstance();
        instance.start(null);
    }

    private static final Log log = Log.getLog(LSApplication.class);

    public static final String ARG_ECLIPSE_KEYRING = "-eclipse.keyring"; // NON-NLS

    @Nullable
    @Override
    public String getDefaultProjectName() {
        return null;
    }

    @Nullable
    @Override
    public Path getDefaultWorkingFolder() {
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBPPlatform> getPlatformClass() {
        return LSPlatform.class;
    }

    @Override
    public boolean isEnvironmentVariablesAccessible() {
        return false;
    }

    // IApplication
    @Override
    public Object start(IApplicationContext context) {
        int status = runApplication();
        if (status != 0) {
            System.exit(status);
        }
        return EXIT_OK;
    }

    private int runApplication() {
        // Command line args definition
        OptionGroup mainOptionGroup = new OptionGroup();
        mainOptionGroup.setRequired(true);
        Option optionVersion = Option.builder()
            .longOpt("version")
            .desc("print version information") //NON-NLS
            .build();
        mainOptionGroup.addOption(optionVersion);
        Option optionHelp = Option.builder()
            .longOpt("help")
            .desc("print help") //NON-NLS
            .build();
        mainOptionGroup.addOption(optionHelp);
        Option optionPort = Option.builder()
            .longOpt("port") //NON-NLS
            .desc("listen for an LSP client on specified port. " //NON-NLS
                + "Note: the server keeps listening for a new LSP client if the old one disconnects") //NON-NLS
            .hasArg()
            .type(Integer.TYPE)
            .build();
        mainOptionGroup.addOption(optionPort);
        Option optionStandardStreams = Option.builder()
            .longOpt("standard-streams") //NON-NLS
            .desc("communicate with the LSP client using stdin/stdout") //NON-NLS
            .build();
        mainOptionGroup.addOption(optionStandardStreams);
        Options cliOptions = new Options();
        cliOptions.addOptionGroup(mainOptionGroup);

        // Parse cli arguments
        CommandLineParser cliParser = new DefaultParser();
        CommandLine commandLine;
        try {
            commandLine = cliParser.parse(cliOptions, getRealApplicationArgs());
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp(cliOptions);
            return 1;
        }

        // Process basic command line options that assume immediate exit
        if (commandLine.hasOption(optionVersion)) {
            System.out.println(GeneralUtils.getLongProductTitle());
            return 0;
        }
        if (commandLine.hasOption(optionHelp)) {
            printHelp(cliOptions);
            return 0;
        }

        // Lock workspace, enable DI
        try {
            Location instanceLocation = Platform.getInstanceLocation();
            if (!instanceLocation.isLocked()) {
                instanceLocation.lock();
            }
        } catch (IOException e) {
            log.error("couldn't lock instance location", e); //NON-NLS
            return 1;
        }
        initializeApplicationServices();

        // Run LSP on standard streams
        if (commandLine.hasOption(optionStandardStreams)) {
            try {
                // This is a blocking call
                DBLFacade.runLanguageServer(System.in, System.out);
                return 0;
            } catch (DBException e) {
                return onExceptionWhenRunningLSP(e);
            }
        }

        // Run LSP on a port. Keep listening for a new LSP client if the old one disconnects.
        // This is basically a debug mode
        String portAsString = commandLine.getOptionValue(optionPort);
        int port;
        try {
            port = Integer.parseInt(portAsString);
        } catch (NumberFormatException e) {
            log.error("application cannot parse port '%s' as int".formatted(portAsString), e); //NON-NLS
            return 1;
        }
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log.info("listening for an LSP client on port " + port); //NON-NLS
                try (Socket socket = serverSocket.accept()) {
                    log.info("a client has connected"); //NON-NLS
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    DBLFacade.runLanguageServer(in, out);
                }
            } catch (IOException | DBException e) {
                return onExceptionWhenRunningLSP(e);
            }
        }
    }

    private static void printHelp(Options cliOptions) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(GeneralUtils.getProductName(), cliOptions);
    }

    private static int onExceptionWhenRunningLSP(Throwable e) {
        log.error("unexpected error when running an LSP server", e); //NON-NLS
        return 1;
    }

    @NotNull
    private static String[] getRealApplicationArgs() {
        return patchApplicationArgs(Platform.getApplicationArgs());
    }

    /**
     * Transforms supplied cli arguments to the form ready to be consumed by CLI argument processors.
     *
     * @param rawApplicationArgs args to transform
     * @return cli arguments transformed to the form ready to be consumed by CLI argument processors
     *
     * @implNote this method exists and made public for testing purposes
     */
    @NotNull
    @ForTest
    private static String[] patchApplicationArgs(String[] rawApplicationArgs) {
        // Remove keyring parameter because its name contains special characters
        // Actual valuation of keyring happens in app launcher
        int idx = ArrayUtils.indexOf(rawApplicationArgs, ARG_ECLIPSE_KEYRING);
        if (idx == -1) {
            return rawApplicationArgs;
        }
        int toIdx = idx;
        if (idx + 1 != rawApplicationArgs.length) {
            toIdx++;
        }
        return ArrayUtils.deleteArea(String.class, rawApplicationArgs, idx, toIdx);
    }
}
