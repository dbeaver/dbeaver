/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.data.office.export;

import org.apache.poi.ss.usermodel.Sheet;


public class Worksheet {
	
	private Sheet sh;
	private Object columnVal;
	private int currentRow;
	
	public Worksheet(Sheet sh, Object columnVal, int currentRow)
	{
		super();
		this.sh = sh;
		this.columnVal = columnVal;
		this.currentRow = currentRow;
	}
	
	public Sheet getSh()
	{
		return sh;
	}
	
	public Object getColumnVal()
	{
		return columnVal;
	}
	
	public int getCurrentRow()
	{
		return currentRow;
	}
	
	public void incRow(){
		currentRow++;
	}
	
	public void dispose(){
		sh = null;
	}
}
