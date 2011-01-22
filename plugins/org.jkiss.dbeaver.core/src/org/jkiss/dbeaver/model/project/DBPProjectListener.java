/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.project;

import org.eclipse.core.resources.IProject;

/**
 * DBPProjectListener
 */
public interface DBPProjectListener
{
    void handleActiveProjectChange(IProject oldValue, IProject newValue);
}
