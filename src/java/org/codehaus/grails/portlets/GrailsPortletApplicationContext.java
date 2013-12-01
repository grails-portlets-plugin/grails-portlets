package org.codehaus.grails.portlets;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;

import org.codehaus.grails.portlets.container.AbstractPortletContainerAdapter;
import org.codehaus.grails.portlets.container.PortletContainerAdapter;
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.portlet.context.ConfigurablePortletApplicationContext;

/**
 * @author Lee Butts
 * @author Kenji Nakamura
 */
public class GrailsPortletApplicationContext extends GrailsWebApplicationContext
   implements ConfigurablePortletApplicationContext {

   private PortletContext portletContext;
   private PortletConfig portletConfig;
   private PortletContainerAdapter portletContainerAdapter = null;

   final Logger logger = LoggerFactory.getLogger(GrailsPortletApplicationContext.class);

   public void setPortletContext(PortletContext portletContext) {
      this.portletContext = portletContext;
      initPortletContainerAdapter(portletContext);
      try {
         setServletContext(portletContainerAdapter.getServletContext(portletContext));
      } catch (UnsupportedOperationException e) {
         logger.warn("Couldn't obtain the underlying servletContext and set to the superclass GrailsWebApplicationContext.");
      }
   }

   private void initPortletContainerAdapter(PortletContext portletContext) {
      portletContainerAdapter = AbstractPortletContainerAdapter.getInstance(portletContext);
   }

   public PortletContext getPortletContext() {
      return portletContext;
   }

   public void setPortletConfig(PortletConfig portletConfig) {
      this.portletConfig = portletConfig;
      initPortletContainerAdapter(portletContext);
      try {
         setServletConfig(portletContainerAdapter.getServletConfig(portletConfig));
      } catch (UnsupportedOperationException e) {
         logger.warn("Couldn't obtain the underlying servletConfig and set to the superclass GrailsWebApplicationContext.");
      }
   }

   public PortletConfig getPortletConfig() {
      return portletConfig;
   }

   public PortletContainerAdapter getPortletContainerAdapter() {
      return portletContainerAdapter;
   }
}
