/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.indent;

public interface Symbols
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

