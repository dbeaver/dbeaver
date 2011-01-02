/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.core.resources.IFile;

/**
 * Local content storage.
 * Content editor may user such content storage directly without copying data to local storage.
 *
 * @author Serge Rider
 */
public interface DBDContentStorageLocal extends DBDContentStorage {

    IFile getDataFile();

}