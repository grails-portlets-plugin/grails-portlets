<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>
<div>
<h1>Help Page</h1>
The map returned by renderView is passed in. Value of mykey: ${mykey}
<form action="${portletResponse.createActionURL()}">
    <input type="submit" value="Done"/>
</form>

</div>
