/*
 * Copyright 2004 The JA-SIG Collaborative. All rights reserved. See license
 * distributed with this file and available online at
 * http://www.uportal.org/license.html
 */
package org.jasig.cas.util.support;

import org.jasig.cas.util.PasswordTranslator;

import junit.framework.TestCase;

/**
 * @author Scott Battaglia
 * @version $Id: Md5PasswordTranslatorTests.java,v 1.2 2005/02/27 05:49:26
 * sbattaglia Exp $
 */
public class Md5PasswordTranslatorTests extends TestCase {

    private PasswordTranslator passwordTranslator = new Md5PasswordTranslator();

    public void testNullPassword() {
        assertEquals(null, this.passwordTranslator.translate(null));
    }

    public void testHash() {
        assertEquals("1f3870be274f6c49b3e31a0c6728957f",
            this.passwordTranslator.translate("apple"));
    }
}
