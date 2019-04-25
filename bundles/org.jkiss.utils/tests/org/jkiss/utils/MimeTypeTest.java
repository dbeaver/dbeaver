package org.jkiss.utils;

import org.jkiss.utils.MimeType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MimeTypeTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();
  @Test
  public void testParse() {
    Assert.assertEquals("application/*", new MimeType("application").toString());
    Assert.assertEquals("/*", new MimeType(";application").toString());
    Assert.assertEquals("application/json", new MimeType("application/json").toString());
    Assert.assertEquals("application/js", new MimeType("application/js;on").toString());

    thrown.expect(IllegalArgumentException.class);
    new MimeType("application;/json");
  }

  @Test
  public void testMatch() {
    Assert.assertTrue(new MimeType().match(new MimeType()));
    Assert.assertFalse(new MimeType().match(new MimeType("text", "json")));
    Assert.assertTrue(new MimeType("application", "json").match(new MimeType("application", "*")));
    Assert.assertFalse(new MimeType("application", "json").match(new MimeType("application", "text")));
    
    Assert.assertTrue(new MimeType("application", "json").match("application/json"));
  }
}
