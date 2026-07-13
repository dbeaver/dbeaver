/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class DBValueFormattingTest extends DBeaverUnitTest {
	
	private DBSObject objectAssociation;
	private DBSObject objectProcedure;
	private DBSEntity objectEntity;
	private DBPObject objectPackage;
	private DBPObject objectTrigger;
	private DBPObject object;

	
	@BeforeEach
	public void setUpAssociationObject() {
		object = mock(DBPObject.class);
		objectAssociation =  mock(DBSEntityAssociation.class);
		objectProcedure =  mock(DBSProcedure.class);
		objectPackage = mock(DBSPackage.class);
		objectTrigger = mock(DBSTrigger.class);		
		objectEntity = mock(DBSEntity.class);
	}

	@Test
	public void testGetObjectImageAssociation() {
        
        Assertions.assertTrue(objectAssociation instanceof DBSEntityAssociation);
        Assertions.assertTrue(objectAssociation instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectAssociation, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TREE_ASSOCIATION,image);        
	}	

	@Test
	public void testGetObjectImageProcedure() {
        
        Assertions.assertTrue(objectProcedure instanceof DBSProcedure);
        Assertions.assertTrue(objectProcedure instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectProcedure, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TREE_PROCEDURE,image);
	}
	
	@Test
	public void testGetObjectImagePackage() {

        Assertions.assertTrue(objectPackage instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectPackage, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TREE_PACKAGE,image);
	}
	
	@Test
	public void testGetObjectImageTrigger() {

        Assertions.assertTrue(objectTrigger instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectTrigger, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TREE_TRIGGER,image);
	}
	
	@Test
	public void testGetObjectImage() {
        
        Assertions.assertTrue(object instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(object, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TYPE_OBJECT,image);
	}
	
	@Test
	public void testGetObjectImageEntity() {
        
        Assertions.assertTrue(objectEntity instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectEntity, true);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(DBIcon.TREE_TABLE,image);
	}
	
	@Test
	public void testGetObjectReturnsNull() {

        DBPImage image = DBValueFormatting.getObjectImage(object, false);
        Assertions.assertNull(image);
	}
}
