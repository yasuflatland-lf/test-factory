package com.liferay.support.tools.service.datalist;

import com.liferay.message.boards.model.MBMessage;
import com.liferay.message.boards.model.MBThread;
import com.liferay.message.boards.service.MBMessageLocalService;
import com.liferay.message.boards.service.MBThreadLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.support.tools.service.DataListProvider;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DataListProvider.class)
public class MBThreadsDataListProvider implements DataListProvider {

	@Override
	public JSONArray getOptions(long companyId, String type) {
		return JSONFactoryUtil.createJSONArray();
	}

	@Override
	public JSONArray getOptions(
			long companyId, String type,
			HttpServletRequest httpServletRequest)
		throws Exception {

		JSONArray jsonArray = JSONFactoryUtil.createJSONArray();

		long groupId = ParamUtil.getLong(httpServletRequest, "groupId", 0);

		if (groupId <= 0) {
			return jsonArray;
		}

		List<MBThread> threads = _mbThreadLocalService.getThreads(
			groupId, 0L, WorkflowConstants.STATUS_ANY, QueryUtil.ALL_POS,
			QueryUtil.ALL_POS);

		for (MBThread thread : threads) {
			MBMessage rootMessage = _mbMessageLocalService.getMessage(
				thread.getRootMessageId());

			jsonArray.put(
				createOption(
					rootMessage.getSubject(), thread.getThreadId()));
		}

		return jsonArray;
	}

	@Override
	public String[] getSupportedTypes() {
		return new String[] {"mb-threads"};
	}

	@Reference
	private MBMessageLocalService _mbMessageLocalService;

	@Reference
	private MBThreadLocalService _mbThreadLocalService;

}
