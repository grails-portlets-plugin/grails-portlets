package org.codehaus.grails.portlets;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

/**
 * @author Lee Butts
 */
public class DefaultGrailsPortletClass extends AbstractInjectableGrailsClass
        implements GrailsPortletClass {


    public DefaultGrailsPortletClass(Class clazz) {
        super(clazz, "Portlet");
    }
}
