/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

/**
 * Database persist action
 */
public interface IDatabasePersistAction {

    String getTitle();

    String getScript();

    String getUndoScript();

}
