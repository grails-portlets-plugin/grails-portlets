package org.codehaus.grails.portlets;

import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;

import javax.portlet.PortletContext;
import javax.portlet.PortletConfig;

/**
 * @author Lee Butts
 */
public class GrailsPortletApplicationContext extends GrailsWebApplicationContext
        implements ConfigurablePortletApplicationContext {
    private PortletContext portletContext;
    private PortletConfig portletConfig;

    public void setPortletContext(PortletContext portletContext) {
        this.portletContext = portletContext;
    }

    public PortletContext getPortletContext() {
        return portletContext;
    }

    public void setPortletConfig(PortletConfig portletConfig) {
        this.portletConfig = portletConfig;
    }

    public PortletConfig getPortletConfig() {
        return portletConfig;
    }
}
