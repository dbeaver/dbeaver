/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Exasol constants
 *
 * @author Karl Griesser
 */
public class ExasolConstants {

    // Display Categories
    public static final String CAT_AUTH = "Authorities";
    public static final String CAT_BASEOBJECT = "Base Object";
    public static final String CAT_DATETIME = "Date & Time";
    public static final String CAT_OWNER = "Owner";
    public static final String CAT_SOURCE = "Source";
    public static final String CAT_PERFORMANCE = "Performance";
    public static final String CAT_STATS = "Statistics";
    public static final String DRV_CLIENT_NAME = "clientname";
    public static final String DRV_CLIENT_VERSION = "clientversion";
    public static final String DRV_QUERYTIMEOUT = "querytimeout";
    public static final String DRV_CONNECT_TIMEOUT = "connecttimeout";
    public static final String DRV_ENCRYPT = DBConstants.INTERNAL_PROP_PREFIX + "encrypt";
    public static final String DRV_BACKUP_HOST_LIST = DBConstants.INTERNAL_PROP_PREFIX + "backupHostList";
    public static final String DRV_USE_BACKUP_HOST_LIST = DBConstants.INTERNAL_PROP_PREFIX + "useBackupHostList";


    public static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
            DBDPseudoAttributeType.ROWID,
            "ROWID",
            "$alias.ROWID",
            null,
            "Unique row identifier",
            true);

    public static final Map<String,String> encoding = new HashMap<String, String>();
    public static final ArrayList<String> encodings = new ArrayList<String>();
    
    public static final ArrayList<String> stringSepModes = new ArrayList<String>();
    
    public static final ArrayList<String> rowSeperators = new ArrayList<String>();
    
    static
    {
    	rowSeperators.add("CRLF");
    	rowSeperators.add("CR");
    	rowSeperators.add("LF");
    }
    
    static
    {
    	stringSepModes.add("AUTO");
    	stringSepModes.add("ALWAYS");
    	stringSepModes.add("NEVER");
    }
    
    static
    {
      encodings.add("UTF-8");
      encoding.put("UTF-8", "UTF-8");
      encoding.put("UTF8", "UTF-8");
      encoding.put("UTF-8", "UTF-8");
      encoding.put("ISO10646/UTF-8", "UTF-8");
      encoding.put("ISO10646/UTF8", "UTF-8");
      
      encodings.add("ASCII");
      encoding.put("ASCII", "ASCII");
      encoding.put("US-ASCII", "ASCII");
      encoding.put("US", "ASCII");
      encoding.put("ISO-IR-6", "ASCII");
      encoding.put("ANSI_X3.4-1968", "ASCII");
      encoding.put("ANSI_X3.4-1986", "ASCII");
      encoding.put("ISO_646.IRV:1991", "ASCII");
      encoding.put("ISO646-US", "ASCII");
      encoding.put("IBM367", "ASCII");
      encoding.put("IBM-367", "ASCII");
      encoding.put("CP367", "ASCII");
      encoding.put("CP-367", "ASCII");
      encoding.put("367", "ASCII");
      
      encodings.add("ISO-8859-1");
      encoding.put("ISO-8859-1", "ISO-8859-1");
      encoding.put("ISO8859-1", "ISO-8859-1");
      encoding.put("ISO88591", "ISO-8859-1");
      encoding.put("LATIN-1", "ISO-8859-1");
      encoding.put("LATIN1", "ISO-8859-1");
      encoding.put("L1", "ISO-8859-1");
      encoding.put("ISO-IR-100", "ISO-8859-1");
      encoding.put("ISO_8859-1:1987", "ISO-8859-1");
      encoding.put("ISO_8859-1", "ISO-8859-1");
      encoding.put("IBM819", "ISO-8859-1");
      encoding.put("IBM-819", "ISO-8859-1");
      encoding.put("CP819", "ISO-8859-1");
      encoding.put("CP-819", "ISO-8859-1");
      encoding.put("819", "ISO-8859-1");
      
      encodings.add("ISO-8859-2");
      encoding.put("ISO-8859-2", "ISO-8859-2");
      encoding.put("ISO8859-2", "ISO-8859-2");
      encoding.put("ISO88592", "ISO-8859-2");
      encoding.put("LATIN-2", "ISO-8859-2");
      encoding.put("LATIN2", "ISO-8859-2");
      encoding.put("L2", "ISO-8859-2");
      encoding.put("ISO-IR-101", "ISO-8859-2");
      encoding.put("ISO_8859-2:1987", "ISO-8859-2");
      encoding.put("ISO_8859-2", "ISO-8859-2");
      
      encodings.add("ISO-8859-3");
      encoding.put("ISO-8859-3", "ISO-8859-3");
      encoding.put("ISO8859-3", "ISO-8859-3");
      encoding.put("ISO88593", "ISO-8859-3");
      encoding.put("LATIN-3", "ISO-8859-3");
      encoding.put("LATIN3", "ISO-8859-3");
      encoding.put("L3", "ISO-8859-3");
      encoding.put("ISO-IR-109", "ISO-8859-3");
      encoding.put("ISO_8859-3:1988", "ISO-8859-3");
      encoding.put("ISO_8859-3", "ISO-8859-3");
      
      encodings.add("ISO-8859-4");
      encoding.put("ISO-8859-4", "ISO-8859-4");
      encoding.put("ISO8859-4", "ISO-8859-4");
      encoding.put("ISO88594", "ISO-8859-4");
      encoding.put("LATIN-4", "ISO-8859-4");
      encoding.put("LATIN4", "ISO-8859-4");
      encoding.put("L4", "ISO-8859-4");
      encoding.put("ISO-IR-110", "ISO-8859-4");
      encoding.put("ISO_8859-4:1988", "ISO-8859-4");
      encoding.put("ISO_8859-4", "ISO-8859-4");
      
      encodings.add("ISO-8859-5");
      encoding.put("ISO-8859-5", "ISO-8859-5");
      encoding.put("ISO8859-5", "ISO-8859-5");
      encoding.put("ISO88595", "ISO-8859-5");
      encoding.put("CYRILLIC", "ISO-8859-5");
      encoding.put("ISO-IR-144", "ISO-8859-5");
      encoding.put("ISO_8859-5:1988", "ISO-8859-5");
      encoding.put("ISO_8859-5", "ISO-8859-5");
      
      encodings.add("ISO-8859-6");
      encoding.put("ISO-8859-6", "ISO-8859-6");
      encoding.put("ISO8859-6", "ISO-8859-6");
      encoding.put("ISO88596", "ISO-8859-6");
      encoding.put("ARABIC", "ISO-8859-6");
      encoding.put("ISO-IR-127", "ISO-8859-6");
      encoding.put("ISO_8859-6:1987", "ISO-8859-6");
      encoding.put("ISO_8859-6", "ISO-8859-6");
      encoding.put("ECMA-114", "ISO-8859-6");
      encoding.put("ASMO-708", "ISO-8859-6");
      
      encodings.add("ISO-8859-7");
      encoding.put("ISO-8859-7", "ISO-8859-7");
      encoding.put("ISO8859-7", "ISO-8859-7");
      encoding.put("ISO88597", "ISO-8859-7");
      encoding.put("GREEK", "ISO-8859-7");
      encoding.put("GREEK8", "ISO-8859-7");
      encoding.put("ISO-IR-126", "ISO-8859-7");
      encoding.put("ISO_8859-7:1987", "ISO-8859-7");
      encoding.put("ISO_8859-7", "ISO-8859-7");
      encoding.put("ELOT_928", "ISO-8859-7");
      encoding.put("ECMA-118", "ISO-8859-7");
      
      encodings.add("ISO-8859-8");
      encoding.put("ISO-8859-8", "ISO-8859-8");
      encoding.put("ISO8859-8", "ISO-8859-8");
      encoding.put("ISO88598", "ISO-8859-8");
      encoding.put("HEBREW", "ISO-8859-8");
      encoding.put("ISO-IR-138", "ISO-8859-8");
      encoding.put("ISO_8859-8:1988", "ISO-8859-8");
      encoding.put("ISO_8859-8", "ISO-8859-8");
      
      encodings.add("ISO-8859-9");
      encoding.put("ISO-8859-9", "ISO-8859-9");
      encoding.put("ISO8859-9", "ISO-8859-9");
      encoding.put("ISO88599", "ISO-8859-9");
      encoding.put("LATIN-5", "ISO-8859-9");
      encoding.put("LATIN5", "ISO-8859-9");
      encoding.put("L5", "ISO-8859-9");
      encoding.put("ISO-IR-148", "ISO-8859-9");
      encoding.put("ISO_8859-9:1989", "ISO-8859-9");
      encoding.put("ISO_8859-9", "ISO-8859-9");
      
      encodings.add("ISO-8859-11");
      encoding.put("ISO-8859-11", "ISO-8859-11");
      encoding.put("ISO8859-11", "ISO-8859-11");
      encoding.put("ISO885911", "ISO-8859-11");
      
      encodings.add("ISO-8859-13");
      encoding.put("ISO-8859-13", "ISO-8859-13");
      encoding.put("ISO8859-13", "ISO-8859-13");
      encoding.put("ISO885913", "ISO-8859-13");
      encoding.put("LATIN-7", "ISO-8859-13");
      encoding.put("LATIN7", "ISO-8859-13");
      encoding.put("L7", "ISO-8859-13");
      encoding.put("ISO-IR-179", "ISO-8859-13");
      
      encodings.add("ISO-8859-15");
      encoding.put("ISO-8859-15", "ISO-8859-15");
      encoding.put("ISO8859-15", "ISO-8859-15");
      encoding.put("ISO885915", "ISO-8859-15");
      encoding.put("LATIN-9", "ISO-8859-15");
      encoding.put("LATIN9", "ISO-8859-15");
      encoding.put("L9", "ISO-8859-15");
      
      encodings.add("IBM850");
      encoding.put("IBM850", "IBM850");
      encoding.put("IBM-850", "IBM850");
      encoding.put("CP850", "IBM850");
      encoding.put("CP-850", "IBM850");
      encoding.put("850", "IBM850");
      
      encodings.add("IBM852");
      encoding.put("IBM852", "IBM852");
      encoding.put("IBM-852", "IBM852");
      encoding.put("CP852", "IBM852");
      encoding.put("CP-852", "IBM852");
      encoding.put("852", "IBM852");
      
      encodings.add("IBM855");
      encoding.put("IBM855", "IBM855");
      encoding.put("IBM-855", "IBM855");
      encoding.put("CP855", "IBM855");
      encoding.put("CP-855", "IBM855");
      encoding.put("855", "IBM855");
      
      encodings.add("IBM856");
      encoding.put("IBM856", "IBM856");
      encoding.put("IBM-856", "IBM856");
      encoding.put("CP856", "IBM856");
      encoding.put("CP-856", "IBM856");
      encoding.put("856", "IBM856");
      
      encodings.add("IBM857");
      encoding.put("IBM857", "IBM857");
      encoding.put("IBM-857", "IBM857");
      encoding.put("CP857", "IBM857");
      encoding.put("CP-857", "IBM857");
      encoding.put("857", "IBM857");
      
      encodings.add("IBM860");
      encoding.put("IBM860", "IBM860");
      encoding.put("IBM-860", "IBM860");
      encoding.put("CP860", "IBM860");
      encoding.put("CP-860", "IBM860");
      encoding.put("860", "IBM860");
      
      encodings.add("IBM861");
      encoding.put("IBM861", "IBM861");
      encoding.put("IBM-861", "IBM861");
      encoding.put("CP861", "IBM861");
      encoding.put("CP-861", "IBM861");
      encoding.put("861", "IBM861");
      encoding.put("CP-IS", "IBM861");
      
      encodings.add("IBM862");
      encoding.put("IBM862", "IBM862");
      encoding.put("IBM-862", "IBM862");
      encoding.put("CP862", "IBM862");
      encoding.put("CP-862", "IBM862");
      encoding.put("862", "IBM862");
      
      encodings.add("IBM863");
      encoding.put("IBM863", "IBM863");
      encoding.put("IBM-863", "IBM863");
      encoding.put("CP863", "IBM863");
      encoding.put("CP-863", "IBM863");
      encoding.put("863", "IBM863");
      
      encodings.add("IBM864");
      encoding.put("IBM864", "IBM864");
      encoding.put("IBM-864", "IBM864");
      encoding.put("CP864", "IBM864");
      encoding.put("CP-864", "IBM864");
      encoding.put("864", "IBM864");
      
      encodings.add("IBM865");
      encoding.put("IBM865", "IBM865");
      encoding.put("IBM-865", "IBM865");
      encoding.put("CP865", "IBM865");
      encoding.put("CP-865", "IBM865");
      encoding.put("865", "IBM865");
      
      encodings.add("IBM866");
      encoding.put("IBM866", "IBM866");
      encoding.put("IBM-866", "IBM866");
      encoding.put("CP866", "IBM866");
      encoding.put("CP-866", "IBM866");
      encoding.put("866", "IBM866");
      
      encodings.add("IBM868");
      encoding.put("IBM868", "IBM868");
      encoding.put("IBM-868", "IBM868");
      encoding.put("CP868", "IBM868");
      encoding.put("CP-868", "IBM868");
      encoding.put("868", "IBM868");
      encoding.put("CP-AR", "IBM868");
      
      encodings.add("IBM869");
      encoding.put("IBM869", "IBM869");
      encoding.put("IBM-869", "IBM869");
      encoding.put("CP869", "IBM869");
      encoding.put("CP-869", "IBM869");
      encoding.put("869", "IBM869");
      encoding.put("CP-GR", "IBM869");
      
      encodings.add("SHIFT-JIS");
      encoding.put("SHIFT-JIS", "SHIFT-JIS");
      encoding.put("SJIS", "SHIFT-JIS");
      
      encodings.add("WINDOWS-1250");
      encoding.put("WINDOWS-1250", "WINDOWS-1250");
      encoding.put("CP1250", "WINDOWS-1250");
      encoding.put("CP-1250", "WINDOWS-1250");
      encoding.put("1250", "WINDOWS-1250");
      encoding.put("MS-EE", "WINDOWS-1250");
      
      encodings.add("WINDOWS-1251");
      encoding.put("WINDOWS-1251", "WINDOWS-1251");
      encoding.put("CP1251", "WINDOWS-1251");
      encoding.put("CP-1251", "WINDOWS-1251");
      encoding.put("1251", "WINDOWS-1251");
      encoding.put("MS-CYRL", "WINDOWS-1251");
      
      encodings.add("WINDOWS-1252");
      encoding.put("WINDOWS-1252", "WINDOWS-1252");
      encoding.put("CP1252", "WINDOWS-1252");
      encoding.put("CP-1252", "WINDOWS-1252");
      encoding.put("1252", "WINDOWS-1252");
      encoding.put("MS-ANSI", "WINDOWS-1252");
      
      encodings.add("WINDOWS-1253");
      encoding.put("WINDOWS-1253", "WINDOWS-1253");
      encoding.put("CP1253", "WINDOWS-1253");
      encoding.put("CP-1253", "WINDOWS-1253");
      encoding.put("1253", "WINDOWS-1253");
      encoding.put("MS-GREEK", "WINDOWS-1253");
      
      encodings.add("WINDOWS-1254");
      encoding.put("WINDOWS-1254", "WINDOWS-1254");
      encoding.put("CP1254", "WINDOWS-1254");
      encoding.put("CP-1254", "WINDOWS-1254");
      encoding.put("1254", "WINDOWS-1254");
      encoding.put("MS-TURK", "WINDOWS-1254");
      
      encodings.add("WINDOWS-1255");
      encoding.put("WINDOWS-1255", "WINDOWS-1255");
      encoding.put("CP1255", "WINDOWS-1255");
      encoding.put("CP-1255", "WINDOWS-1255");
      encoding.put("1255", "WINDOWS-1255");
      encoding.put("MS-HEBR", "WINDOWS-1255");
      
      encodings.add("WINDOWS-1256");
      encoding.put("WINDOWS-1256", "WINDOWS-1256");
      encoding.put("CP1256", "WINDOWS-1256");
      encoding.put("CP-1256", "WINDOWS-1256");
      encoding.put("1256", "WINDOWS-1256");
      encoding.put("MS-ARAB", "WINDOWS-1256");
      
      encodings.add("WINDOWS-1257");
      encoding.put("WINDOWS-1257", "WINDOWS-1257");
      encoding.put("CP1257", "WINDOWS-1257");
      encoding.put("CP-1257", "WINDOWS-1257");
      encoding.put("1257", "WINDOWS-1257");
      encoding.put("WINBALTRIM", "WINDOWS-1257");
      
      encodings.add("WINDOWS-1258");
      encoding.put("WINDOWS-1258", "WINDOWS-1258");
      encoding.put("CP1258", "WINDOWS-1258");
      encoding.put("CP-1258", "WINDOWS-1258");
      encoding.put("1258", "WINDOWS-1258");
      
      encodings.add("WINDOWS-874");
      encoding.put("WINDOWS-874", "WINDOWS-874");
      encoding.put("CP874", "WINDOWS-874");
      encoding.put("CP-874", "WINDOWS-874");
      encoding.put("874", "WINDOWS-874");
      encoding.put("IBM874", "WINDOWS-874");
      encoding.put("IBM-874", "WINDOWS-874");
      
      encodings.add("WINDOWS-31J");
      encoding.put("WINDOWS-31J", "WINDOWS-31J");
      encoding.put("WINDOWS-932", "WINDOWS-31J");
      encoding.put("CP932", "WINDOWS-31J");
      encoding.put("CP-932", "WINDOWS-31J");
      encoding.put("932", "WINDOWS-31J");
      
      encodings.add("WINDOWS-936");
      encoding.put("WINDOWS-936", "WINDOWS-936");
      encoding.put("CP936", "WINDOWS-936");
      encoding.put("CP-936", "WINDOWS-936");
      encoding.put("936", "WINDOWS-936");
      encoding.put("GBK", "WINDOWS-936");
      encoding.put("MS936", "WINDOWS-936");
      encoding.put("MS-936", "WINDOWS-936");
      
      encodings.add("CP949");
      encoding.put("CP949", "CP949");
      encoding.put("WINDOWS-949", "CP949");
      encoding.put("CP-949", "CP949");
      encoding.put("949", "CP949");
      
      encodings.add("BIG5");
      encoding.put("BIG5", "BIG5");
      encoding.put("WINDOWS-950", "BIG5");
      encoding.put("CP950", "BIG5");
      encoding.put("CP-950", "BIG5");
      encoding.put("950", "BIG5");
      encoding.put("BIG", "BIG5");
      encoding.put("BIG5", "BIG5");
      encoding.put("BIG-5", "BIG5");
      encoding.put("BIG-FIVE", "BIG5");
      encoding.put("BIGFIVE", "BIG5");
      encoding.put("CN-BIG5", "BIG5");
      encoding.put("BIG5-CP950", "BIG5");
    }
    
}
