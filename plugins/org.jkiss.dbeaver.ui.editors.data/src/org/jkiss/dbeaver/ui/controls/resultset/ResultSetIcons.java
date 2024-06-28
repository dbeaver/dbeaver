/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBIcon;

/**
 * Plan Icons
 */
public class ResultSetIcons {

    public static final DBIcon META_KEY_NA = new DBIcon("meta_key_na", "meta_key_na.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon META_KEY_OK = new DBIcon("meta_key_ok", "meta_key_ok.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon META_KEY_VIRTUAL = new DBIcon("meta_key_virt", "meta_key_virt.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon META_TABLE_NA = new DBIcon("meta_table_na", "meta_table_na.png"); //$NON-NLS-1$ //$NON-NLS-2$

    static  {
        DBIcon.loadIcons(ResultSetIcons.class);
    }
}
