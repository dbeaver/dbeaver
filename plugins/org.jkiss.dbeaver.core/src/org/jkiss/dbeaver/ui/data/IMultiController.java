/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data;

/**
 * Multi-value controller.
 * Supports value editors switch.
 */
public interface IMultiController
{
    /**
     * Closes current value editor.
     * This action may initiated by editor control (e.g. on Enter or Esc key)
     */
    void closeInlineEditor();

    /**
     * Closes current editor and activated next cell editor
     * @param next true for next and false for previous cell
     */
    void nextInlineEditor(boolean next);

}
