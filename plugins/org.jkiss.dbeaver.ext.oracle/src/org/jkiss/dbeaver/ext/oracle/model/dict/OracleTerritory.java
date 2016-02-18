/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
 * NLS territory dictionary
 */
public enum OracleTerritory
{
	ALGERIA("ALGERIA"),
	AMERICA("AMERICA"),
	AUSTRALIA("AUSTRALIA"),
	AUSTRIA("AUSTRIA"),
	BAHRAIN("BAHRAIN"),
	BANGLADESH("BANGLADESH"),
	BELGIUM("BELGIUM"),
	BRAZIL("BRAZIL"),
	BULGARIA("BULGARIA"),
	CANADA("CANADA"),
	CATALONIA("CATALONIA"),
	CHINA("CHINA"),
	CIS("CIS"),
	CROATIA("CROATIA"),
	CYPRUS("CYPRUS"),
	CZECH("CZECH"),
	CZECHOSLOVAKIA("CZECHOSLOVAKIA"),
	DENMARK("DENMARK"),
	DJIBOUTI("DJIBOUTI"),
	EGYPT("EGYPT"),
	ESTONIA("ESTONIA"),
	FINLAND("FINLAND"),
	FRANCE("FRANCE"),
	GERMANY("GERMANY"),
	GREECE("GREECE"),
	HONG_KONG("HONG KONG"),
	HUNGARY("HUNGARY"),
	ICELAND("ICELAND"),
	INDONESIA("INDONESIA"),
	IRAQ("IRAQ"),
	IRELAND("IRELAND"),
	ISRAEL("ISRAEL"),
	ITALY("ITALY"),
	JAPAN("JAPAN"),
	JORDAN("JORDAN"),
	KAZAKHSTAN("KAZAKHSTAN"),
	KOREA("KOREA"),
	KUWAIT("KUWAIT"),
	LATVIA("LATVIA"),
	LEBANON("LEBANON"),
	LIBYA("LIBYA"),
	LITHUANIA("LITHUANIA"),
	LUXEMBOURG("LUXEMBOURG"),
	MALAYSIA("MALAYSIA"),
	MAURITANIA("MAURITANIA"),
	MEXICO("MEXICO"),
	MOROCCO("MOROCCO"),
	NEW_ZEALAND("NEW ZEALAND"),
	NORWAY("NORWAY"),
	OMAN("OMAN"),
	POLAND("POLAND"),
	PORTUGAL("PORTUGAL"),
	QATAR("QATAR"),
	ROMANIA("ROMANIA"),
	SAUDI_ARABIA("SAUDI ARABIA"),
	SINGAPORE("SINGAPORE"),
	SLOVAKIA("SLOVAKIA"),
	SLOVENIA("SLOVENIA"),
	SOMALIA("SOMALIA"),
	SOUTH_AFRICA("SOUTH AFRICA"),
	SPAIN("SPAIN"),
	SUDAN("SUDAN"),
	SWEDEN("SWEDEN"),
	SWITZERLAND("SWITZERLAND"),
	SYRIA("SYRIA"),
	TAIWAN("TAIWAN"),
	THAILAND("THAILAND"),
	THE_NETHERLANDS("THE NETHERLANDS"),
	TUNISIA("TUNISIA"),
	TURKEY("TURKEY"),
	UKRAINE("UKRAINE"),
	UNITED_ARAB_EMIRATES("UNITED ARAB EMIRATES"),
	UNITED_KINGDOM("UNITED KINGDOM"),
	UZBEKISTAN("UZBEKISTAN"),
	VIETNAM("VIETNAM"),
	YEMEN("YEMEN");


    private final String territory;

    OracleTerritory(String territory)
    {
        this.territory = territory;
    }

    public String getTerritory()
    {
        return territory;
    }
}
