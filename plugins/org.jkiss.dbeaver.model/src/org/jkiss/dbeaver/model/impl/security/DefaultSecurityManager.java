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
package org.jkiss.dbeaver.model.impl.security;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPSecurityManager;

import java.io.File;
import java.security.KeyStore;

/**
 * DefaultSecurityManager
 */
public class DefaultSecurityManager implements DBPSecurityManager {

    static final Log log = Log.getLog(DefaultSecurityManager.class);

    private final File localPath;

    public DefaultSecurityManager(File localPath) {
        this.localPath = localPath;
        if (!localPath.exists() && !localPath.mkdirs()) {
            log.error("Can't create directory for security manager: " + localPath.getAbsolutePath());
        }
    }

    @Override
    public KeyStore getKeyStore(String ksId) {
        return null;
    }

    @Override
    public File getKeyStorePath(String ksId) {
        return null;
    }
}