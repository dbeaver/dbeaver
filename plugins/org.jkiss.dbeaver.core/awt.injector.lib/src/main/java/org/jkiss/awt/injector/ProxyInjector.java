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
import java.awt.peer.DesktopPeer;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ProxyInjector {
    public void injectBrowseInteraction(
        Callable<Boolean> isBrowseSupportedCallable,
        Function<URI, Boolean> browseFunction
    ) throws NoSuchFieldException, IllegalAccessException {
        Desktop desktop = Desktop.getDesktop();
        Class<? extends Desktop> desktopClass = desktop.getClass();
        Field peerField = desktopClass.getDeclaredField("peer");

        peerField.setAccessible(true);
        Object peerObject = peerField.get(desktop);
        if (peerObject != null) {
            DesktopPeer peer = (DesktopPeer) peerObject;
            peerField.set(desktop, new BrowsePeerProxy(peer, isBrowseSupportedCallable, browseFunction));
        }
    }
}
