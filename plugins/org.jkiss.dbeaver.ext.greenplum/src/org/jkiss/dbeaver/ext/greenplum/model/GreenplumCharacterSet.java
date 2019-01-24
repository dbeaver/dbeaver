/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.greenplum.model;

/**
 * GreenplumCharacterSet
 *
 * Supported as of Greenplum 5.x (http://gpdb.docs.pivotal.io/500/ref_guide/character_sets.html)
 */
public enum GreenplumCharacterSet {
    BIG_FIVE("BIG5"),
    EXTENDED_UNIX_CODE_CN("EUC_CN"),
    EXTENDED_UNIX_CODE_JP("EUC_JP"),
    EXTENDED_UNIX_CODE_KR("EUC_KR"),
    EXTENDED_UNIX_CODE_TW("EUC_TW"),
    NATIONAL_STANDARD("GB18030"),
    EXTENDED_NATIONAL_STANDARD("GBK"),
    ISO_8859_5("ISO_8859_5"),
    ISO_8859_6("ISO_8859_6"),
    ISO_8859_7("ISO_8859_7"),
    ISO_8859_8("ISO_8859_8"),
    JOHAB("JOHA"),
    KOI8_R("KOI8"),
    ISO_8859_1("LATIN1"),
    ISO_8859_2("LATIN2"),
    ISO_8859_3("LATIN3"),
    ISO_8859_4("LATIN4"),
    ISO_8859_9("LATIN5"),
    ISO_8859_10("LATIN6"),
    ISO_8859_13("LATIN7"),
    ISO_8859_14("LATIN8"),
    ISO_8859_15("LATIN9"),
    ISO_8859_16("LATIN10"),
    MULE_INTERNAL_CODE("MULE_INTERNAL"),
    SHIFT_JIS("SJIS"),
    SQL_ASCII("SQL_ASCII"),
    UNIFIED_HANGUL_CODE("UHC"),
    UNICODE_8BIT("UTF8"),
    WINDOWS_CP866("WIN866"),
    WINDOWS_CP874("WIN874"),
    WINDOWS_CP1250("WIN1250"),
    WINDOWS_CP1251("WIN1251"),
    WINDOWS_CP1252("WIN1252"),
    WINDOWS_CP1253("WIN1253"),
    WINDOWS_CP1254("WIN1254"),
    WINDOWS_CP1255("WIN1255"),
    WINDOWS_CP1256("WIN1256"),
    WINDOWS_CP1257("WIN1257"),
    WINDOWS_CP1258("WIN1258");

    private final String characterSetValue;

    GreenplumCharacterSet(String characterSet) {
        this.characterSetValue = characterSet;
    }

    public String getCharacterSetValue() {
        return this.characterSetValue;
    }
}
