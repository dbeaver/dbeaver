/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
