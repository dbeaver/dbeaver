/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
