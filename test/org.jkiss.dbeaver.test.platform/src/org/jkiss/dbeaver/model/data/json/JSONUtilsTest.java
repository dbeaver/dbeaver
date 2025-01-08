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

package org.jkiss.dbeaver.model.data.json;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class JSONUtilsTest extends DBeaverUnitTest {

	private Map<String, Object> map;
	private Map<String, Object> map2;
	private Map<String, Object> nestedMap1;
	private Map<String, Object> nestedMap2;
	private Map<String, Object> map3;
	private List<Map<String, Object>> list;

	@Before
	public void setUp() {
		map = new HashMap<>();
		map.put("Location", "London");
		map.put("Time", null);

		map2 = new HashMap<>();
		nestedMap1 = new HashMap<>();
		nestedMap1.put("US", "New York");
		map2.put("Location", nestedMap1);

		nestedMap2 = new HashMap<>();
		nestedMap2.put("Apple", "iPhone");
		map2.put("Product", nestedMap2);

		map2.put("Time", null);

		map3 = new HashMap<>();
		Map<String, Object> nestedMap3 = new HashMap<>();
		nestedMap3.put("UK", "London");
		Map<String, Object> nestedMap4 = new HashMap<>();
		nestedMap4.put("FR", "Paris");
		list = new ArrayList<>();
		list.add(nestedMap1);
		list.add(nestedMap3);
		list.add(nestedMap4);
		map3.put("Location", list);
		map3.put("Time", 10);

	}

	@Test
	public void parseDateTest() {
		Assert.assertEquals(null, JSONUtils.parseDate(null));
		Assert.assertEquals(new Date(((Number) 22).longValue()), JSONUtils.parseDate(((Number) 22)));
		Assert.assertEquals(new Date(((Number) Long.MAX_VALUE).longValue()), JSONUtils.parseDate(Long.MAX_VALUE));

		Exception exception = Assert.assertThrows(IllegalArgumentException.class, () -> {
			JSONUtils.parseDate(((Number) 99.88));
		});
		String expectedMessage = "Cannot parse date from value '99.88'";
		String actualMessage = exception.getMessage();
		Assert.assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	public void getStringTest() {
		Assert.assertEquals("London", JSONUtils.getString(map, "Location"));
		Assert.assertEquals(null, JSONUtils.getString(map, "Time"));
	}

	@Test
	public void getObjectTest() {
		Assert.assertEquals(nestedMap1, JSONUtils.getObject(map2, "Location"));
		Assert.assertEquals(nestedMap2, JSONUtils.getObject(map2, "Product"));
		Assert.assertEquals(new LinkedHashMap<>(), JSONUtils.getObject(map2, "Time"));
	}

	@Test
	public void getObjectListTest() {
		Assert.assertEquals(list, JSONUtils.getObjectList(map3, "Location"));
		Assert.assertEquals(Collections.emptyList(), JSONUtils.getObjectList(map3, "Time"));
	}

}
