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

package org.jkiss.dbeaver.ui.editors.sql.indent;

public interface SQLIndentSymbols
{
    int TokenEOF   = -1;
    int TokenOTHER = 0;
    int Tokenbegin = 1000;
    int TokenBEGIN = 1001;
    int Tokenend = 1002;
    int TokenEND = 1003;
    int TokenIDENT = 2000;


    String BEGIN = "BEGIN";
    String begin = "begin";
    String beginTrail = "end ";
    String BEGINTrail = "END ";
}

