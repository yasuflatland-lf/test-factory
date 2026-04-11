package com.liferay.support.tools.portlet.actions;

import com.goikosoft.crawler4j.crawler.CrawlConfig;
import com.goikosoft.crawler4j.crawler.CrawlController;
import com.goikosoft.crawler4j.fetcher.PageFetcher;
import com.goikosoft.crawler4j.robotstxt.RobotstxtConfig;
import com.goikosoft.crawler4j.robotstxt.RobotstxtServer;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.support.tools.constants.LDFPortletKeys;
import com.liferay.support.tools.utils.ImageCrawler;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;

@Component(
	property = {
		"javax.portlet.name=" + LDFPortletKeys.LIFERAY_DUMMY_FACTORY,
		"mvc.command.name=/ldf/image/list"
	},
	service = MVCResourceCommand.class
)
public class ImageLinkResourceCommand extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		JSONObject responseJson = JSONFactoryUtil.createJSONObject();

		try {
			int numberOfCrawlers = ParamUtil.getInteger(
				resourceRequest, "numberOfCrawlers", 1);
			int maxDepthOfCrawling = ParamUtil.getInteger(
				resourceRequest, "maxDepthOfCrawling", 2);
			int maxPagesToFetch = ParamUtil.getInteger(
				resourceRequest, "maxPagesToFetch", 10);
			int randomAmount = ParamUtil.getInteger(
				resourceRequest, "randomAmount", 10);

			String tmpUrls = ParamUtil.getString(
				resourceRequest, "urls",
				"https://imgur.com/search?q=flower");

			List<String> urls = new ArrayList<>(
				Arrays.asList(tmpUrls.split(",")));

			List<String> result = Lists.newArrayList();

			if (_log.isDebugEnabled()) {
				_log.debug("numberOfCrawlers : " + numberOfCrawlers);
				_log.debug("maxDepthOfCrawling : " + maxDepthOfCrawling);
				_log.debug("maxPagesToFetch : " + maxPagesToFetch);
			}

			if ((numberOfCrawlers >= 0) && (maxDepthOfCrawling >= 0) &&
				(maxPagesToFetch >= 0)) {

				result = _runCrawl(
					numberOfCrawlers, maxDepthOfCrawling, maxPagesToFetch,
					urls, randomAmount
				).blockingSingle();
			}

			responseJson.put("success", true);
			responseJson.put(
				"urlstr", String.join(LDFPortletKeys.EOL, result));

			JSONArray linksArray = JSONFactoryUtil.createJSONArray();

			for (String url : result) {
				linksArray.put(url);
			}

			responseJson.put("links", linksArray);
		}
		catch (Throwable throwable) {
			_log.error("Failed to crawl image links", throwable);

			ResourceCommandUtil.setErrorResponse(responseJson, throwable);
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, responseJson);
	}

	private List<String> _crawlDomain(
			int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch,
			String domain, int randomAmount)
		throws Exception {

		CrawlConfig config = new CrawlConfig();

		File tempDir = Files.createTempDir();

		config.setCrawlStorageFolder(tempDir.getAbsolutePath());
		config.setPolitenessDelay(1000);
		config.setMaxDepthOfCrawling(maxDepthOfCrawling);
		config.setMaxPagesToFetch(maxPagesToFetch);
		config.setIncludeBinaryContentInCrawling(true);
		config.setIncludeHttpsPages(true);

		PageFetcher pageFetcher = new PageFetcher(config);

		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(
			robotstxtConfig, pageFetcher);

		CrawlController controller = new CrawlController(
			config, pageFetcher, robotstxtServer);

		controller.addSeed(domain);

		ImageCrawler.configure(domain, randomAmount);

		controller.startNonBlocking(ImageCrawler.class, numberOfCrawlers);

		controller.waitUntilFinish();

		List<Object> crawlersLocalData = controller.getCrawlersLocalData();

		List<String> gatheredURLs = new ArrayList<>();

		for (Object localData : crawlersLocalData) {
			@SuppressWarnings("unchecked")
			List<String> urlLists = (List<String>)localData;

			gatheredURLs.addAll(urlLists);
		}

		return gatheredURLs;
	}

	private Flowable<List<String>> _getImagesFromURL(
		int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch,
		String url, int randomAmount) {

		return Flowable.just(
			url
		).concatMap(
			u -> Flowable.just(
				_crawlDomain(
					numberOfCrawlers, maxDepthOfCrawling, maxPagesToFetch, u,
					randomAmount))
		).subscribeOn(
			Schedulers.io()
		);
	}

	private Flowable<ArrayList<String>> _runCrawl(
		int numberOfCrawlers, int maxDepthOfCrawling, int maxPagesToFetch,
		List<String> urls, int randomAmount) {

		return Flowable.fromIterable(
			urls
		).concatMap(
			url -> _getImagesFromURL(
				numberOfCrawlers, maxDepthOfCrawling, maxPagesToFetch, url,
				randomAmount)
		).collectInto(
			new ArrayList<String>(), ArrayList::addAll
		).toFlowable(
		).subscribeOn(
			Schedulers.io()
		);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImageLinkResourceCommand.class);

}
