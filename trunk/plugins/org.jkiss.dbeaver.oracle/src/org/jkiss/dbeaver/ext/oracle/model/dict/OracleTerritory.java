/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
