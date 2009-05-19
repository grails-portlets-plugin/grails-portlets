package org.codehaus.grails.portlets;

import org.codehaus.grails.portlets.container.AbstractPortletContainerAdapter;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Kenji Nakamura
 */
public class GrailsPortletRequest extends GrailsWebRequest {

    private PortletRequest portletRequest;

    public GrailsPortletRequest(PortletRequest portletRequest, PortletResponse portletResponse, ServletContext servletContext) {
        super(AbstractPortletContainerAdapter.getInstance(portletRequest).getHttpServletRequest(portletRequest),
              AbstractPortletContainerAdapter.getInstance(portletResponse).getHttpServletResponse(portletResponse), servletContext);
        this.portletRequest = portletRequest;
    }

    public PortletRequest getPortletRequest() {
        return portletRequest;
    }

}
