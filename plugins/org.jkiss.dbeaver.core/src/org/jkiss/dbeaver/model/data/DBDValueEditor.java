/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * DBD Value Editor.
 * Must be implemented by all visual value editors (dialogs, editors, inline controls).
 */
public interface DBDValueEditor
{
    DBDValueController getValueController();

    /**
     * Brings editor to the top of screen
     */
    void showValueEditor();

    /**
     * Closes this editor.
     * Implementor must call removeEditor on it's value controller
     */
    void closeValueEditor();

}
