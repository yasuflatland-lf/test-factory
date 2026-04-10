<%@ include file="/init.jsp" %>

<portlet:resourceURL id="/ldf/org" var="actionResourceURL" />
<portlet:resourceURL id="/ldf/data" var="dataResourceURL" />

<react:component
	module="{App} from liferay-dummy-factory"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"actionResourceURL", actionResourceURL
		).put(
			"dataResourceURL", dataResourceURL
		).put(
			"namespace", renderResponse.getNamespace()
		).put(
			"progressResourceURL", ""
		).build()
	%>'
/>
