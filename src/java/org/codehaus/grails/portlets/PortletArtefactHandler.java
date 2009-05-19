package org.codehaus.grails.portlets;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

/**
 * @author Lee Butts
 */
public class PortletArtefactHandler extends ArtefactHandlerAdapter{

    public static final String TYPE = "Portlet";

    public PortletArtefactHandler() {
        super(TYPE, GrailsPortletClass.class, DefaultGrailsPortletClass.class, TYPE);
    }


}
