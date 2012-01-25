/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.registry.OSDescriptor;

import java.io.File;

/**
 * DBPDriver local path
 */
public interface DBPDriverFile
{
    DBPDriverFileType getType();

    OSDescriptor getSystem();

    String getPath();

    String getDescription();

    boolean isCustom();

    String getExternalURL();

    boolean isDisabled();

    boolean isLocal();

    File getFile();

    boolean matchesCurrentPlatform();

}
