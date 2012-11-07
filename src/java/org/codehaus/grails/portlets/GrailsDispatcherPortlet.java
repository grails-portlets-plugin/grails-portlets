package org.codehaus.grails.portlets;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.web.portlet.DispatcherPortlet;

/**
 * @author Lee Butts
 */
public class GrailsDispatcherPortlet extends DispatcherPortlet {

   public static final String PORTLET_NAME = "grails.portlet.name";
   public static final String PORTLET_CONFIG = "grails.portlet.config";
   public static final String PORTLET_CLASS_PARAM = "grailsPortletClass";

   @Override
   protected void doActionService(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
      addPortletInfoToRequest(actionRequest);
      super.doActionService(actionRequest, actionResponse);
   }

   @Override
   protected void doEventService(EventRequest eventRequest, EventResponse eventResponse) throws Exception {
	   addPortletInfoToRequest(eventRequest);
	   super.doEventService(eventRequest, eventResponse);
   }

   @Override
   protected void doRenderService(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {
      addPortletInfoToRequest(renderRequest);
      super.doRenderService(renderRequest, renderResponse);
   }

   @Override
   protected void doResourceService(ResourceRequest resourceRequest, ResourceResponse resourceResponse) throws Exception {
	   addPortletInfoToRequest(resourceRequest);
	   super.doResourceService(resourceRequest, resourceResponse);
    }

   private void addPortletInfoToRequest(PortletRequest portletRequest) {
      addPortletNameToRequest(portletRequest);
      addPortletConfigToRequest(portletRequest);
   }

   private void addPortletConfigToRequest(PortletRequest portletRequest) {
      portletRequest.setAttribute(PORTLET_CONFIG, getPortletConfig());
   }

   private void addPortletNameToRequest(PortletRequest request) {
      request.setAttribute(PORTLET_NAME, getPortletConfig().getPortletName());
   }
}
