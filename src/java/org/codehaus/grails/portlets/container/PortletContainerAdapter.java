package org.codehaus.grails.portlets.container;

import javax.portlet.PortletContext;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides container agnositc access to the underlying ServletContext and
 * ServletConfig.
 *
 * @author Kenji Nakamura
 * @since 0.2
 */
public interface PortletContainerAdapter {

    /**
     * Returns the underlying ServletContext
     *
     * @param context portlet context
     * @return
     * @throws UnsupportedOperationException thrown when there is no way to retrieve the context from the
     *                                       portlet context
     */
    public ServletContext getServletContext(PortletContext context) throws UnsupportedOperationException;

    /**
     * Returns the underlying ServletConfig
     *
     * @param config portlet config
     * @return
     * @throws UnsupportedOperationException thrown when the operation is not possible with the underlying portlet container
     */
    public ServletConfig getServletConfig(PortletConfig config) throws UnsupportedOperationException;

    /**
     * Returns the underlying HttpServletRequest.
     *
     * @param portletRequest portlet request
     * @return http servlet request
     * @throws UnsupportedOperationException thrown when the operation is not possible with the underlying portlet container
     */
    public HttpServletRequest getHttpServletRequest(PortletRequest portletRequest) throws UnsupportedOperationException;


    /**
     * Returns the underlying HttpServletResponse.
     *
     * @param portletResponse portlet response
     * @return http servlet request
     * @throws UnsupportedOperationException thrown when the operation is not possible with the underlying portlet container
     */
    public HttpServletResponse getHttpServletResponse(PortletResponse portletResponse) throws UnsupportedOperationException;

}
