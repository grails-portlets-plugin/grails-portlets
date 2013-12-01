package org.codehaus.grails.portlets;

import groovy.lang.GroovyObject;

import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;

import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.handler.AbstractHandlerMapping;

/**
 * Handler mapping to look up Portlet class for request
 */
public class GrailsPortletHandlerMapping extends AbstractHandlerMapping {

   protected Object getHandlerInternal(PortletRequest portletRequest) throws Exception {

      PortletConfig portletConfig = (PortletConfig) portletRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_CONFIG);
      String portletName = null;
      if (portletConfig != null) {
    	  portletName = portletConfig.getInitParameter(GrailsDispatcherPortlet.PORTLET_CLASS_PARAM);
      }
      if (portletName == null) {
         portletName = portletRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME) + "Portlet";
      }

      GroovyObject portlet = (GroovyObject) getApplicationContext().getBean(portletName);
      return new HandlerExecutionChain(portlet);
   }
}
