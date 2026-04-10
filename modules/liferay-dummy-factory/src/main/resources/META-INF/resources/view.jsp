<%@ include file="/init.jsp" %>

<portlet:resourceURL id="/ldf/calculate" var="calculateURL" />

<react:component
	module="{Calculator} from liferay-dummy-factory"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"calculateURL", calculateURL
		).put(
			"namespace", renderResponse.getNamespace()
		).build()
	%>'
/>
