package com.bekk.boss.pluto.embedded.jetty.util;

import org.mortbay.jetty.servlet.AbstractSessionManager;
import org.mortbay.jetty.servlet.HashSessionManager;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;

/**
 * @author Nils-Helge Garli
 */
public class PlutoJettySessionManager extends HashSessionManager {

    public Cookie access(HttpSession session, boolean secure) {
        if (session instanceof AbstractSessionManager.Session) {
            return super.access(session, secure);
        } else {
            try {
                Field sessionField = session.getClass().getDeclaredField(
                        "httpSession");
                sessionField.setAccessible(true);
                HttpSession sess = (HttpSession) sessionField.get(session);
                return super.access(sess, secure);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * @author Lee Butts
     */
    public void complete(HttpSession session) {
        if (session instanceof AbstractSessionManager.Session) {
            super.complete(session);
        } 
    }
}
