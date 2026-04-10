package com.liferay.support.tools.portlet.actions;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.service.DataListProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/data"
	},
	service = MVCResourceCommand.class
)
public class DataListResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		HttpServletRequest httpServletRequest =
			_portal.getOriginalServletRequest(
				_portal.getHttpServletRequest(resourceRequest));

		String type = ParamUtil.getString(httpServletRequest, "type");

		long companyId = _portal.getCompanyId(httpServletRequest);

		DataListProvider provider = _providers.get(type);

		JSONArray jsonArray;

		if (provider != null) {
			jsonArray = provider.getOptions(companyId, type);
		}
		else {
			_log.warn("Unknown data list type requested: " + type);

			jsonArray = JSONFactoryUtil.createJSONArray();
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, jsonArray);
	}

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		unbind = "_removeProvider"
	)
	private void _addProvider(DataListProvider provider) {
		for (String type : provider.getSupportedTypes()) {
			_providers.put(type, provider);
		}
	}

	private void _removeProvider(DataListProvider provider) {
		for (String type : provider.getSupportedTypes()) {
			_providers.remove(type);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DataListResourceCommand.class);

	@Reference
	private Portal _portal;

	private final Map<String, DataListProvider> _providers =
		new ConcurrentHashMap<>();

}
