@artifact.package@
import javax.portlet.*

class @artifact.name@ {

  def title = 'Portlet Title'
  def description = '''
Description about the portlet goes here.
'''
  def displayName = 'Display Name'
  def supports = ['text/html':['view', 'edit', 'help']]
  
  // Used for liferay
  // @see http://www.liferay.com/documentation/liferay-portal/6.0/development/-/ai/anatomy-of-a-portlet
  // def liferay_display_category = "category.sample"

  def actionEdit = {
    //TODO Define action phase
    portletResponse.setPortletMode(PortletMode.VIEW) 
  }

  def renderEdit = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }

  def actionView = {
    //TODO Define action phase
  }

  def renderView = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }

  def actionHelp = {
    //TODO Define action phase
    portletResponse.setPortletMode(PortletMode.VIEW) 
  }

  def renderHelp = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }
}
