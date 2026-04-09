<%@ include file="/init.jsp" %>

<react:component
	module="{Calculator} from test-factory-calculator"
	props='<%=
		HashMapBuilder.<String, Object>put(
			"namespace", liferayPortletResponse.getNamespace()
		).build()
	%>'
/>
