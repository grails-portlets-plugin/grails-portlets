package org.codehaus.grails.portlets.container;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Kenji Nakamura
 */
public abstract class AbstractPortletContainerAdapter implements PortletContainerAdapter, ApplicationContextAware {

   private static ApplicationContext ctx;

   /**
    * Instantiate an appropriate implementation based on the package name of the
    * parameter.
    *
    * @param obj Any portlet container specific implemntation. The package name is used.
    * @return an implemntation of this class suitable for the underlying portlet container.
    */
   public static PortletContainerAdapter getInstance(Object obj) {
      return (PortletContainerAdapter) ctx.getBean("portletContainerAdapter");
   }

   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      ctx = applicationContext;
   }
}
