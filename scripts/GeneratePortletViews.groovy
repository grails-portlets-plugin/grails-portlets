includeTargets << grailsScript("Init")
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsGenerate")

target('main': "Generates the portal views for a specified portal class") {
   depends(checkVersion, parseArguments, packageApp)
   promptForName(type: "Portlet Class")
   portletClass = grails.util.GrailsNameUtils.getShortName(argsMap["params"][0])
   portletClass = portletClass.substring(0, 1).toLowerCase() + portletClass.substring(1)
   def viewsDir = "${basedir}/grails-app/views/${portletClass}"
   ant.copy(todir: viewsDir, overwrite: false) {
      fileset(dir: "${portletsPluginDir}/src/templates/portlet-views")
   }
}

setDefaultTarget(main)



