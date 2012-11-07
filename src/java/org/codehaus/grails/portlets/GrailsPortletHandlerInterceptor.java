package org.codehaus.grails.portlets;

import org.codehaus.grails.portlets.container.AbstractPortletContainerAdapter;
import org.codehaus.grails.portlets.container.PortletContainerAdapter;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.portlet.handler.HandlerInterceptorAdapter;

import javax.portlet.*;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * This class is needed to modify the request/set context holders after
 * DispatcherPortlet has done it's thing. Otherwise this code could go in the
 * GrailsDispatcherPortlet
 * 
 * @author Lee Butts
 */
public class GrailsPortletHandlerInterceptor extends HandlerInterceptorAdapter
        implements ServletContextAware {
	private ServletContext servletContext;

	@Override
	protected boolean preHandle(PortletRequest request,
	        PortletResponse response, Object handler) throws Exception {
		LocaleContextHolder.setLocale(request.getLocale());
		convertRequestToGrailsWebRequest(request, response);
		return true;
	}

	@Override
    protected void afterCompletion(PortletRequest request,
            PortletResponse response, Object handler, Exception ex)
            throws Exception {
    	GrailsPortletRequest webRequest = (GrailsPortletRequest) request
    	        .getAttribute(GrailsApplicationAttributes.WEB_REQUEST);
    	webRequest.requestCompleted();
    	request.removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);
    	PortletContainerAdapter portletContainerAdapter = AbstractPortletContainerAdapter
    	        .getInstance(request);
    	HttpServletRequest servletRequest = portletContainerAdapter
    	        .getHttpServletRequest(request);
    	servletRequest.removeAttribute(GrailsApplicationAttributes.WEB_REQUEST);
    	RequestContextHolder.setRequestAttributes(null);
    	LocaleContextHolder.setLocale(null);
    }

	private void convertRequestToGrailsWebRequest(
	        PortletRequest portletRequest, PortletResponse portletResponse) {
		GrailsPortletRequest webRequest = new GrailsPortletRequest(
		        portletRequest, portletResponse, servletContext);
		RequestContextHolder.setRequestAttributes(webRequest);
		portletRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST,
		        webRequest);
		PortletContainerAdapter portletContainerAdapter = AbstractPortletContainerAdapter
		        .getInstance(portletRequest);
		HttpServletRequest servletRequest = portletContainerAdapter
		        .getHttpServletRequest(portletRequest);
		servletRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST,
		        webRequest);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}
}
