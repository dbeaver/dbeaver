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

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.impl.StreamExporterAbstract;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Export XLSX with Apache POI
 *
 */
public class DataExporterXLSX extends StreamExporterAbstract{

    private static final String PROP_HEADER = "header";
    private static final String PROP_NULL_STRING = "nullString";

    private static final String PROP_ROWNUMBER = "rownumber";
    private static final String PROP_BORDER = "border";
    private static final String PROP_HEADER_FONT = "headerfont";
    
    private static final String BINARY_FIXED = "[BINARY]";
    
    private static final String PROP_TRUESTRING = "trueString";
    private static final String PROP_FALSESTRING = "falseString";
    
    enum FontStyleProp  {NONE, BOLD, ITALIC, STRIKEOUT, UNDERLINE}
    
    private static final int ROW_WINDOW = 100;
    
    private String nullString;

    private List<DBDAttributeBinding> columns;
    
    private SXSSFWorkbook wb;    
    private Sheet sh;
	
    private boolean printHeader = false;
    private boolean rowNumber = false;
    private String boolTrue="YES";
	private String boolFalse="NO";
	
	private int rowIndex;
	private XSSFCellStyle style;
	private XSSFCellStyle styleHeader;
    
    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        Object nullStringProp = site.getProperties().get(PROP_NULL_STRING);        
        nullString = nullStringProp == null ? null : nullStringProp.toString();
        
        try {
            
        	printHeader = (Boolean) site.getProperties().get(PROP_HEADER);
            
        } catch (Exception e) {
        	
            printHeader = false;
            
        }
        

        try {
            
        	rowNumber = (Boolean) site.getProperties().get(PROP_ROWNUMBER);
            
        } catch (Exception e) {
        	
        	rowNumber = false;
            
        }

        try {
            
        	boolTrue = (String) site.getProperties().get(PROP_TRUESTRING);
            
        } catch (Exception e) {
        	
        	boolTrue = "YES";
            
        }

        try {
            
        	boolFalse = (String) site.getProperties().get(PROP_FALSESTRING);
            
        } catch (Exception e) {
        	
        	boolTrue = "NO";
            
        }

        
	    wb = new SXSSFWorkbook(ROW_WINDOW);
	    
        sh = wb.createSheet();

        rowIndex =0;
        
        styleHeader = (XSSFCellStyle) wb.createCellStyle();
        
        
        BorderStyle border;
        
        try {
            
        	border = BorderStyle.valueOf((String) site.getProperties().get(PROP_BORDER));
            
        } catch (Exception e) {
        	
            border = BorderStyle.NONE;
            
        }
        
        FontStyleProp fontStyle;
        
        try {
            
        	fontStyle = FontStyleProp.valueOf((String) site.getProperties().get(PROP_HEADER_FONT));
            
        } catch (Exception e) {
        	
        	fontStyle = FontStyleProp.NONE;
            
        }

        
        styleHeader.setBorderTop(border); 
        styleHeader.setBorderBottom(border);
        styleHeader.setBorderLeft(border);
        styleHeader.setBorderRight(border);
        
        XSSFFont fontBold = (XSSFFont) wb.createFont();
        
        switch (fontStyle) {
        
		case BOLD:
			fontBold.setBold(true);	
			break;

		case ITALIC:
			fontBold.setItalic(true);
			break;
			
		case STRIKEOUT:
			fontBold.setStrikeout(true);
			break;
			
		case UNDERLINE:
			fontBold.setUnderline((byte) 3);
			break;
			
		default:
			break;
		}
        
        styleHeader.setFont(fontBold);
        
        style = (XSSFCellStyle) wb.createCellStyle();
        style.setBorderTop(border); 
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);

       
        super.init(site);
    }

    @Override
    public void dispose()
    {
    	
      	try {
			wb.write(getSite().getOutputStream());
	      	wb.dispose();
		} catch (IOException e) {			
			e.printStackTrace();			
		}
    	
    	
    	sh = null;
    	wb = null;
        super.dispose();
    }
    
	@Override
	public void exportHeader(DBCSession session) throws DBException, IOException
	{
		
	        columns = getSite().getAttributes();
	        
	        if (printHeader) {
	            printHeader();
	        }
		
	}

	 private void printHeader()
	    {
		    Row row = sh.createRow(rowIndex);
		    
		    int startCol = rowNumber ? 1 : 0;
		    
	        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
	            DBDAttributeBinding column = columns.get(i);
	            String colName = column.getLabel();
	            if (CommonUtils.isEmpty(colName)) {
	                colName = column.getName();
	            }
	            Cell cell = row.createCell(i+startCol);
                cell.setCellValue(colName); 
                cell.setCellStyle(styleHeader); 
	        }
	        rowIndex = 1;
	    }


 

	 private void writeCellValue(Cell cell,Reader reader) throws IOException
	    {
	        try {
	        	StringBuilder sb = new StringBuilder();
	            char buffer[] = new char[2000];
	            for (;;) {
	                int count = reader.read(buffer);
	                if (count <= 0) {
	                    break;
	                }
	                sb.append(buffer, 0, count);
	            }
	            if (sb.length() > 0) {
	            	cell.setCellValue(sb.toString());
	            }
	        } finally {
	            ContentUtils.close(reader);
	        }
	    } 
	 

	 
	@Override
	public void exportRow(DBCSession session, Object[] row)
			throws DBException, IOException
	{
		
		Row rowX = sh.createRow(rowIndex);
		
		int startCol = 0;
		
		if (rowNumber) {
			
			Cell cell = rowX.createCell(startCol);
			cell.setCellStyle(style);
			cell.setCellValue(String.valueOf(rowIndex));
			startCol++;
		}
		
        for (int i = 0; i < row.length; i++) {
        	Cell cell = rowX.createCell(i+startCol);	                
        	cell.setCellStyle(style);
            DBDAttributeBinding column = columns.get(i); 
            
            if (DBUtils.isNullValue(row[i])) {
                if (!CommonUtils.isEmpty(nullString)) {
                	cell.setCellValue(nullString); 	                
                }
            } else if (row[i] instanceof DBDContent) {
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                    	cell.setCellValue(DBConstants.NULL_VALUE_LABEL); 
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cell,cs.getContentReader()); 
                    } else {
                    	cell.setCellValue(BINARY_FIXED);
                    }
                }
                finally {
                    content.release();
                }
            } else if (row[i] instanceof Boolean) {
             
            	cell.setCellValue((Boolean) row[i] ? boolTrue : boolFalse);
            	
            } else {            	
            
                    String stringValue = super.getValueDisplayString(column, row[i]);
                    cell.setCellValue(stringValue);
            }
           
        }
        rowIndex++;
		
	}

	@Override
	public void exportFooter(DBRProgressMonitor monitor)
			throws DBException, IOException
	{
		
	}



}
