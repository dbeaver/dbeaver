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
package org.jkiss.dbeaver.ext.oracle.model.dict;

/**
 * NLS language dictionary
 */
public enum OracleLanguage
{
	AMERICAN("AMERICAN"),
	ARABIC("ARABIC"),
	BENGALI("BENGALI"),
	BRAZILIAN_PORTUGUESE("BRAZILIAN PORTUGUESE  "),
	BULGARIAN("BULGARIAN"),
	CANADIAN_FRENCH("CANADIAN FRENCH"),
	CATALAN("CATALAN"),
	SIMPLIFIED_CHINESE ("SIMPLIFIED CHINESE "),
	CROATIAN("CROATIAN"),
	CZECH("CZECH"),
	DANISH("DANISH"),
	DUTCH("DUTCH"),
	EGYPTIAN("EGYPTIAN"),
	ENGLISH("ENGLISH"),
	ESTONIAN("ESTONIAN"),
	FINNISH("FINNISH"),
	FRENCH("FRENCH"),
	GERMAN_DIN("GERMAN DIN  "),
	GERMAN("GERMAN"),
	GREEK("GREEK"),
	HEBREW("HEBREW  "),
	HUNGARIAN("HUNGARIAN"),
	ICELANDIC("ICELANDIC"),
	INDONESIAN("INDONESIAN"),
	ITALIAN("ITALIAN"),
	JAPANESE("JAPANESE"),
	KOREAN("KOREAN"),
	LATIN_AMERICAN_SPANISH("LATIN AMERICAN SPANISH  "),
	LATVIAN("LATVIAN"),
	LITHUANIAN("LITHUANIAN"),
	MALAY("MALAY  "),
	MEXICAN_SPANISH("MEXICAN SPANISH"),
	NORWEGIAN("NORWEGIAN"),
	POLISH("POLISH"),
	PORTUGUESE("PORTUGUESE"),
	ROMANIAN  ("ROMANIAN  "),
	RUSSIAN("RUSSIAN"),
	SLOVAK("SLOVAK"),
	SLOVENIAN  ("SLOVENIAN  "),
	SPANISH("SPANISH"),
	SWEDISH("SWEDISH"),
	THAI("THAI  "),
	TRADITIONAL_CHINESE("TRADITIONAL CHINESE"),
	TURKISH("TURKISH"),
	UKRAINIAN("UKRAINIAN"),
	VIETNAMESE("VIETNAMESE");

    private final String language;

    OracleLanguage(String language)
    {
        this.language = language;
    }

    public String getLanguage()
    {
        return language;
    }
}
