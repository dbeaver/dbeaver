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
package org.jkiss.awt.injector;

import java.awt.*;
import java.awt.desktop.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.awt.peer.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import javax.swing.*;

class BrowsePeerProxy implements DesktopPeer {
    private final DesktopPeer peer;
    private final Function<URI, Boolean> browseFunction;
    private final Callable<Boolean> isBrowseSupportedCallable;

    BrowsePeerProxy(
        DesktopPeer peer,
        Callable<Boolean> isBrowseSupportedInteraction,
        Function<URI, Boolean> browseFunction
    ) {
        this.peer = peer;
        this.isBrowseSupportedCallable = isBrowseSupportedInteraction;
        this.browseFunction = browseFunction;
    }

    @Override
    public boolean isSupported(Desktop.Action action) {
        try {
            Boolean success = this.isBrowseSupportedCallable.call();
            if (success) {
                return true;
            }
        } catch (Exception ignored) {
            // ignored
        }
        return peer.isSupported(action);

    }

    @Override
    public void open(File file) throws IOException {
        peer.open(file);
    }

    @Override
    public void edit(File file) throws IOException {
        peer.print(file);
    }

    @Override
    public void print(File file) throws IOException {
        peer.print(file);
    }

    @Override
    public void mail(URI mailtoURL) throws IOException {
        peer.mail(mailtoURL);
    }

    @Override
    public void browse(URI uri) throws IOException {
        try {
            Boolean success = this.browseFunction.apply(uri);
            if (success) {
                return;
            }
        } catch (Exception ignored) {
            // ignored
        }
        peer.browse(uri);
    }

    @Override
    public void addAppEventListener(SystemEventListener listener) {
        peer.addAppEventListener(listener);
    }

    @Override
    public void removeAppEventListener(SystemEventListener listener) {
        peer.removeAppEventListener(listener);
    }

    @Override
    public void setAboutHandler(AboutHandler aboutHandler) {
        peer.setAboutHandler(aboutHandler);
    }

    @Override
    public void setPreferencesHandler(PreferencesHandler preferencesHandler) {
        peer.setPreferencesHandler(preferencesHandler);
    }

    @Override
    public void setOpenFileHandler(OpenFilesHandler openFileHandler) {
        peer.setOpenFileHandler(openFileHandler);
    }

    @Override
    public void setPrintFileHandler(PrintFilesHandler printFileHandler) {
        peer.setPrintFileHandler(printFileHandler);
    }

    @Override
    public void setOpenURIHandler(OpenURIHandler openURIHandler) {
        peer.setOpenURIHandler(openURIHandler);
    }

    @Override
    public void setQuitHandler(QuitHandler quitHandler) {
        peer.setQuitHandler(quitHandler);
    }

    @Override
    public void setQuitStrategy(QuitStrategy strategy) {
        peer.setQuitStrategy(strategy);
    }

    @Override
    public void enableSuddenTermination() {
        peer.enableSuddenTermination();
    }

    @Override
    public void disableSuddenTermination() {
        peer.disableSuddenTermination();
    }

    @Override
    public void requestForeground(boolean allWindows) {
        peer.requestForeground(allWindows);
    }

    @Override
    public void openHelpViewer() {
        peer.openHelpViewer();
    }

    @Override
    public void setDefaultMenuBar(JMenuBar menuBar) {
        peer.setDefaultMenuBar(menuBar);
    }

    @Override
    public boolean browseFileDirectory(File file) {
        return peer.browseFileDirectory(file);
    }

    @Override
    public boolean moveToTrash(File file) {
        return peer.moveToTrash(file);
    }
}

