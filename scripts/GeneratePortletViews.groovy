import grails.util.GrailsNameUtils

includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsGenerate")

target(generatePortletViews: "Generates the portal views for a specified portal class") {
   depends(checkVersion, parseArguments, packageApp)

   promptForName(type: "Portlet Class")

   portletClass = GrailsNameUtils.getShortName(argsMap.params[0])
	portletClass = portletClass.substring(0, 1).toLowerCase() + portletClass.substring(1)

   String viewsDir = "${basedir}/grails-app/views/${portletClass}"

   ant.copy(todir: viewsDir, overwrite: false) {
		fileset(dir: "${portletsPluginDir}/src/templates/portlet-views")
   }
}

setDefaultTarget(generatePortletViews)
