package org.codehaus.grails.portlets;

import groovy.lang.GroovyObject;
import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.HandlerMapping;
import org.springframework.web.portlet.handler.AbstractHandlerMapping;

import javax.portlet.PortletRequest;

/**
 * Handler mapping to look up Portlet class for request
 */
public class GrailsPortletHandlerMapping extends
        AbstractHandlerMapping implements HandlerMapping {

    protected Object getHandlerInternal(PortletRequest portletRequest) throws Exception {
       String portletName = (String) portletRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME);
       GroovyObject portlet = (GroovyObject) getApplicationContext().getBean(portletName + "Portlet");
       return new HandlerExecutionChain(portlet);
    }                                                                               
}
