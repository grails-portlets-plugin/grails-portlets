package org.codehaus.grails.portlets;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.View;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import java.util.Map;

/**
 * @author Lee Butts
 */
public class GrailsPortletHandlerAdapter implements org.springframework.web.portlet.HandlerAdapter,
        ApplicationContextAware {
    private ApplicationContext applicationContext;

    public boolean supports(Object o) {
        return o instanceof GroovyObject;
    }

    public void handleAction(ActionRequest actionRequest, ActionResponse actionResponse, Object o) throws Exception {
        GroovyObject portlet = (GroovyObject) o;
        Closure action = (Closure) portlet.getProperty("doAction");
        action.call();
    }

    public ModelAndView handleRender(RenderRequest renderRequest, RenderResponse renderResponse, Object o) throws Exception {
        //TODO if window mode is minimized should I just render empty string for them?
        GroovyObject portlet = (GroovyObject) o;
        Closure render = (Closure) portlet.getProperty("doRender");
        Object returnValue = render.call();
        if (returnValue instanceof Map) {
            renderRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER, portlet);
            String modeView = "/" + ((String) renderRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME)).toLowerCase()
                    + "/" + renderRequest.getPortletMode().toString().toLowerCase();
            if (tryResolveView(modeView)) {
                return new ModelAndView(modeView, (Map) returnValue);
            } else {
                String renderView = "/" + ((String) renderRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME)).toLowerCase()
                        + "/render";
                return new ModelAndView(renderView, (Map) returnValue);
            }
        } else {
            return null;
        }
    }

    public boolean tryResolveView(String viewName) {
        ViewResolver vr = (ViewResolver) applicationContext.getBean("jspViewResolver");
        try {
            View view = vr.resolveViewName(viewName, LocaleContextHolder.getLocaleContext().getLocale());
            return view instanceof GroovyPageView; // GrailsViewResolver will return a GPV if it exists otherwise it's a normal JSP view (which may or may not exist)
        } catch (Exception e) {
            return false;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
