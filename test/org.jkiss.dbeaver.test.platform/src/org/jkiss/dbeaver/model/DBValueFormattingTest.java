/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class DBValueFormattingTest extends DBeaverUnitTest {
	
	private DBSObject objectAssociation;
	private DBSObject objectProcedure;
	private DBSEntity objectEntity;
	private DBPObject objectPackage;
	private DBPObject objectTrigger;
	private DBPObject object;

	
	@Before
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
        
        Assert.assertTrue(objectAssociation instanceof DBSEntityAssociation);
        Assert.assertTrue(objectAssociation instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectAssociation, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TREE_ASSOCIATION,image);        
	}	

	@Test
	public void testGetObjectImageProcedure() {
        
        Assert.assertTrue(objectProcedure instanceof DBSProcedure);
        Assert.assertTrue(objectProcedure instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectProcedure, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TREE_PROCEDURE,image);
	}
	
	@Test
	public void testGetObjectImagePackage() {

        Assert.assertTrue(objectPackage instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectPackage, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TREE_PACKAGE,image);
	}
	
	@Test
	public void testGetObjectImageTrigger() {

        Assert.assertTrue(objectTrigger instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectTrigger, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TREE_TRIGGER,image);
	}
	
	@Test
	public void testGetObjectImage() {
        
        Assert.assertTrue(object instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(object, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TYPE_OBJECT,image);
	}
	
	@Test
	public void testGetObjectImageEntity() {
        
        Assert.assertTrue(objectEntity instanceof DBPObject);
        DBPImage image = DBValueFormatting.getObjectImage(objectEntity, true);
        Assert.assertNotNull(image);
        Assert.assertEquals(DBIcon.TREE_TABLE,image);
	}
	
	@Test
	public void testGetObjectReturnsNull() {

        DBPImage image = DBValueFormatting.getObjectImage(object, false);
        Assert.assertNull(image);
	}
}
