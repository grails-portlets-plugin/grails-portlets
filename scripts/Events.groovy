import grails.util.GrailsUtil
import groovy.xml.MarkupBuilder
import org.mortbay.jetty.Server
import org.mortbay.jetty.security.HashUserRealm
import org.mortbay.jetty.security.UserRealm
import org.mortbay.jetty.webapp.WebAppContext
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

def version = 0.1
def basedir = System.getProperty("base.dir")
def portletXml = new File("${basedir}/web-app/web-inf/portlet.xml")
def pluginLibDir = "${basedir}/plugins/portlets-${version}/lib"
def plutoVersion = '1.1.4'
def plutoConfigXml = new File("${pluginLibDir}/pluto-portal-${plutoVersion}/web-inf/pluto-portal-driver-config.xml")
def confClassList = ["org.mortbay.jetty.webapp.WebInfConfiguration",
        "org.mortbay.jetty.plus.webapp.EnvConfiguration",
        "org.mortbay.jetty.plus.webapp.Configuration",
        "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
        "org.mortbay.jetty.webapp.TagLibConfiguration"]

eventConfigureJetty = {Server server ->
    def grailsContext = server.getHandler()
    grailsContext.systemClasses = ["org.apache.pluto.", "org.castor.", "javax.portlet.", "org.springframework."]
   
    def webContext = new WebAppContext("${pluginLibDir}/pluto-portal-${plutoVersion}", "pluto")

    System.setProperty('java.endorsed.dirs', "${pluginLibDir}/endorsed")
    System.setProperty('jetty.class.path', "${pluginLibDir}/castor-1.1.1.jar;" +
            "${pluginLibDir}/pluto-container-1.1.4.jar;" +
            "${pluginLibDir}/pluto-descriptor-api-1.1.4.jar;" +
            "${pluginLibDir}/pluto-descriptor-impl-1.1.4.jar;" +
            "${pluginLibDir}/pluto-taglib-1.1.4.jar;" +
            "${pluginLibDir}/portlet-api-1.0.jar;")

    webContext.systemClasses = ["-org.apache.pluto.driver","org.apache.pluto.", "org.castor.", "javax.portlet.", "org.springframework."]
    webContext.contextPath = "/pluto"
    server.addHandler(webContext)

    HashUserRealm myrealm = new HashUserRealm("default", "${pluginLibDir}/realm.properties");
    server.setUserRealms([myrealm] as UserRealm[]);
}

eventPackagingEnd = {
    try {
        def sw = new StringWriter();
        def xmlWriter = new MarkupBuilder(sw);
        def searchPath = "file:${basedir}/grails-app/controllers/**/*Portlet.groovy"
        event("StatusUpdate", ["Searching for portlets: ${searchPath}"])
        portletFiles = resolveResources(searchPath).toList()
        if (portletFiles.size() > 0) {
            event("StatusUpdate", ["Generating portlet.xml - ${portletFiles.size()} portlets found"])

            if (portletXml.exists()) portletXml.delete()
            xmlWriter.'portlet-app'(version: '1.0',
                    xmlns: 'http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd',
                    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                    'xsi:schemaLocation': 'http://java.sun.com/xml/ns/portlet/portlet-app_1_0.xsd') {
                portletFiles.each {portletClassFile ->
                    def className = portletClassFile.filename - '.groovy'
                    Class portletClass = classLoader.loadClass(className)
                    def portletName = className - 'Portlet'
                    def instance = portletClass.newInstance()
                    checkRequiredProperties(['supports', 'title'], instance)
                    //TODO security role refs, security constraints, user attributes, il8n
                    xmlWriter.portlet {
                        'portlet-name'(portletName)
                        if (hasProperty('displayName', instance))
                            'display-name'(instance.displayName)
                        if (hasProperty('description', instance))
                            'description'(instance.description)
                        'portlet-class'('org.codehaus.grails.portlets.GrailsDispatcherPortlet')
                        'init-param' {
                            'name'('contextClass')
                            'value'('org.codehaus.grails.portlets.GrailsPortletApplicationContext')
                        }
                        'init-param' {
                            'name'('grailsPortletClass')
                            'value'(className)
                        }
                        'supports' {
                            'mime-type'('*/*')
                            instance.supports.each {mode ->
                                'portlet-mode'(mode)
                            }
                        }
                        'portlet-info' {
                            'title'(instance.title)
                            if (hasProperty('shortTitle', instance)) 'short-title'(instance.shortTitle)
                            if (hasProperty('keywords', instance)) 'keywords'(instance.keywords)
                        }
                        if (hasProperty('preferences', instance) && instance.preferences instanceof Map) {
                            'portlet-preferences' {
                                instance.preferences.each {prefName, prefValue ->
                                    'preference' {
                                        'name'(prefName)
                                        if (prefValue instanceof List) {
                                            prefValue.each {multiValue ->
                                                'value'(multiValue)
                                            }
                                        } else {
                                            'value'(prefValue)
                                        }
                                        /* TODO
                                        if (preference.readOnly) {
                                            'read-only'('true')
                                        }*/
                                    }
                                }
                            }
                        }
                    }
                }
            }
            portletXml.write(sw.toString())
            if (GrailsUtil.isDevelopmentEnv()) {
                sw = new StringWriter()
                xmlWriter = new MarkupBuilder(sw)
                if (plutoConfigXml.exists()) plutoConfigXml.delete()
                xmlWriter.'pluto-portal-driver'(
                        'xmlns': "http://portals.apache.org/pluto/xsd/pluto-portal-driver-config.xsd",
                        'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
                        'xsi:schemaLocation': "http://portals.apache.org/pluto/xsd/pluto-portal-driver-config.xsd",
                        'version': "1.1") {
                    'portal-name'('pluto-portal-driver')
                    'portal-version'(plutoVersion)
                    'container-name'('Pluto Portal Driver')
                    'supports' {
                        'portlet-mode'('view')
                        'portlet-mode'('edit')
                        'portlet-mode'('help')
                        'portlet-mode'('config')

                        'window-state'('normal')
                        'window-state'('maximized')
                        'window-state'('minimized')
                    }
                    'render-config'(default: 'Home') {
                        'page'(name: "Home", uri: "/WEB-INF/themes/pluto-default-theme.jsp") {
                            portletFiles.each {portletClassFile ->
                                def className = portletClassFile.filename - '.groovy'
                                def portletName = className - 'Portlet'
                                'portlet'(context: "/${grailsAppName}", name: portletName)
                            }
                        }
                        'page'(name: "About Apache Pluto", uri: "/WEB-INF/themes/pluto-default-theme.jsp") {
                            'portlet'(context: '/pluto', name: 'AboutPortlet')
                        }
                    }
                }
                plutoConfigXml.write(sw.toString())
            }
        }

    } catch (Exception e) {
        event("StatusError", ["Unable to generate portlet.xml: " + e.message])
        exit(1)
    }

}

def hasProperty(propertyName, instance) {
    try {
        def value = instance."${propertyName}"
        return true;
    } catch (MissingPropertyException mpe) {
        return false;
    }
}

def checkRequiredProperties(propertyNames, instance) {
    propertyNames.each {
        if (!hasProperty(it, instance)) {
            throw new MissingPropertyException("${instance.class.name} does not have the required properties ${propertyNames}",
                    instance.class)
        }
    }
}

def resolveResources(String pattern) {
    //try {
    def resolver = new PathMatchingResourcePatternResolver()
    return resolver.getResources(pattern)
    /*}
    catch (Throwable e) {
        e.print
        return []
    } */
}
