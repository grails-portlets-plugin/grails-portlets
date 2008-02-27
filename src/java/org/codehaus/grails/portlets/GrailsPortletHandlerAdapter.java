package org.codehaus.grails.portlets;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.springframework.web.portlet.ModelAndView;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.util.Map;

/**
 * @author Lee Butts
 */
public class GrailsPortletHandlerAdapter implements org.springframework.web.portlet.HandlerAdapter {

    public boolean supports(Object o) {
        return o instanceof GroovyObject;
    }

    public void handleAction(ActionRequest actionRequest, ActionResponse actionResponse, Object o) throws Exception {
        GroovyObject portlet = (GroovyObject) o;
        Closure action = (Closure) portlet.getProperty("doAction");
        action.call();
    }

    public ModelAndView handleRender(RenderRequest renderRequest, RenderResponse renderResponse, Object o) throws Exception {
        GroovyObject portlet = (GroovyObject) o;
        Closure render = (Closure) portlet.getProperty("doRender");
        Object returnValue = render.call();
        if (returnValue instanceof Map) {
            renderRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER, portlet);
            return new ModelAndView("/" + ((String) renderRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME)).toLowerCase()
                    + "/render", (Map) returnValue);
        } else {
            return null;
        }
    }

}
