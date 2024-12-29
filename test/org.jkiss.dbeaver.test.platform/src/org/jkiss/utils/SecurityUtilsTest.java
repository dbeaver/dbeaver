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
package org.jkiss.utils;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

//@RunWith(PowerMockRunner.class)
public class SecurityUtilsTest extends DBeaverUnitTest {

    @Test
    public void dummyTest() throws Exception {

    }

/*
  @PrepareForTest(SecurityUtils.class)
  @Test
  public void testGenerateGUID() throws Exception {
    PowerMockito.mockStatic(InetAddress.class); 
    final InetAddress inetAddress = PowerMockito.mock(InetAddress.class); 
    PowerMockito.when(inetAddress.getHostName()).thenReturn("localhost"); 
    PowerMockito.when(InetAddress.getLocalHost()).thenReturn(inetAddress); 

    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.currentTimeMillis()).thenReturn(100L);

    final SecureRandom secureRand = PowerMockito.mock(SecureRandom.class);
    PowerMockito.whenNew(SecureRandom.class).withNoArguments().thenReturn(secureRand);
    PowerMockito.when(secureRand.nextLong()).thenReturn(0L);

    final Random random = PowerMockito.mock(Random.class);
    PowerMockito.when(random.nextLong()).thenReturn(0L);

    Assert.assertEquals("826ACF9F-7283-7548-3C25-044D0AC2477B", SecurityUtils.generateGUID(true));
    Assert.assertEquals("214F4B5E-FA60-A781-43E1-E58F9EF1808C", SecurityUtils.generateGUID(false));

    PowerMockito.when(inetAddress.getLocalHost()).thenThrow(new UnknownHostException());
    PowerMockito.mockStatic(MessageDigest.class);
    PowerMockito.when(MessageDigest.getInstance(anyString())).thenThrow(new NoSuchAlgorithmException());
    PowerMockito.doAnswer((i) -> { return null; }).when(random).nextBytes(any(byte[].class));
    Assert.assertEquals("E4983893-E6DF-7079-B59B-6888EBD6ADC1", SecurityUtils.generateGUID(true));
  }


  @PrepareForTest(SecurityUtils.class) 
  @Test
  public void testGenerateUniqueId() throws Exception {
    final SecureRandom random = PowerMockito.mock(SecureRandom.class);
    PowerMockito.whenNew(SecureRandom.class).withNoArguments().thenReturn(random);
    PowerMockito.spy(System.class);
    PowerMockito.when(System.currentTimeMillis()).thenReturn(100L);

    PowerMockito.when(random.nextInt(anyInt())).thenReturn(1);
    Assert.assertEquals("2sdukq4l", SecurityUtils.generateUniqueId());

    PowerMockito.when(random.nextInt(anyInt())).thenReturn(-1);
    Assert.assertEquals("2sk6q0t3", SecurityUtils.generateUniqueId());
  }

  @PrepareForTest(SecurityUtils.class)
  @Test
  public void testMakeDigest() throws Exception {
    Assert.assertEquals("6AtQFwmJUPxYqtg8jBSXjg==", SecurityUtils.makeDigest("abc", "def"));
    Assert.assertEquals("kAFQmDzST7DWlj99KOF/cg==", SecurityUtils.makeDigest("abc", null));
    Assert.assertEquals("kAFQmDzST7DWlj99KOF/cg==", SecurityUtils.makeDigest("abc"));

    PowerMockito.mockStatic(MessageDigest.class);
    PowerMockito.when(MessageDigest.getInstance(anyString())).thenThrow(new NoSuchAlgorithmException());
    Assert.assertEquals("*", SecurityUtils.makeDigest("abc", "def"));
    Assert.assertEquals("*", SecurityUtils.makeDigest("abc"));
  }

  @PrepareForTest(SecurityUtils.class)
  @Test
  public void testGenerateRandomLong() throws Exception {
    final SecureRandom random = PowerMockito.mock(SecureRandom.class);
    PowerMockito.whenNew(SecureRandom.class).withNoArguments().thenReturn(random);
    PowerMockito.when(random.nextLong()).thenReturn(0L);
    Assert.assertEquals(0L, SecurityUtils.generateRandomLong());
  }

  @PrepareForTest(SecurityUtils.class) 
  @Test
  public void testGenerateRandomBytes() throws Exception {
    final SecureRandom random = PowerMockito.mock(SecureRandom.class);
    PowerMockito.whenNew(SecureRandom.class).withNoArguments().thenReturn(random);
    PowerMockito.doAnswer((i) -> { return null; }).when(random).nextBytes(any(byte[].class));
    Assert.assertArrayEquals(new byte[]{(byte)0, (byte)0, (byte)0}, SecurityUtils.generateRandomBytes(3));
  }

  @PrepareForTest(SecurityUtils.class) 
  @Test
  public void testGeneratePassword() throws Exception {
    final SecureRandom random = PowerMockito.mock(SecureRandom.class);
    PowerMockito.whenNew(SecureRandom.class).withNoArguments().thenReturn(random);
    PowerMockito.when(random.nextInt(anyInt())).thenReturn(0, 1, 2, 3, 4, 5, 6, 7);
    Assert.assertEquals("ABCDEFGH", SecurityUtils.generatePassword());
  }

  @PrepareForTest(SecurityUtils.class) 
  @Test
  public void testGetRandom() throws Exception {
    final Random random = PowerMockito.mock(Random.class);
    PowerMockito.whenNew(Random.class).withParameterTypes(long.class).withArguments(anyLong()).thenReturn(random);
    Assert.assertEquals(new Random(0L), SecurityUtils.getRandom());
  }
*/
}
