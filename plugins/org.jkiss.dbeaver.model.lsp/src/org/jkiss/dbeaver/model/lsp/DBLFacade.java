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
package org.jkiss.dbeaver.model.lsp;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A facade for accessing all LSP functionality from this package.
 */
public final class DBLFacade {
    private static final Log log = Log.getLog(DBLFacade.class);

    private DBLFacade() {
    }

    /**
     * Runs an LSP using specified streams as a means of communication between a client and a server.
     * This is a blocking call.
     *
     * @param in an InputStream with messages for the server
     * @param out an OutputStream with messages for the client
     */
    public static void runLanguageServer(
        @NotNull InputStream in,
        @NotNull OutputStream out,
        @Nullable DBLServerSessionProvider sessionProvider
    ) throws DBException {
        try {
            log.info("Launching LSP server"); //NON-NLS
            DBLServer server = new DBLServer(sessionProvider);
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);
            LanguageClient client = launcher.getRemoteProxy();
            server.connect(client);
            Future<Void> launcherFuture = launcher.startListening();
            launcherFuture.get();
            log.info("the LSP client has closed the stream. " + client); //NON-NLS
        } catch (InterruptedException | ExecutionException e) {
            throw new DBException("unexpected exception while running LSP server", e);
        }
    }
}
