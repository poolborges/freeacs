package com.github.freeacs.web.app.page.monitor;

import com.github.freeacs.dbi.ACS;
import com.github.freeacs.web.app.Output;
import com.github.freeacs.web.app.input.ParameterParser;
import com.github.freeacs.web.app.page.AbstractWebPage;
import com.github.freeacs.web.app.util.ACSLoader;
import com.github.freeacs.web.app.util.WebConstants;
import com.github.freeacs.web.app.util.WebProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for retrieving and displaying the output from the Monitor Server.
 * Migrated to Apache HttpClient 5.
 *
 * @author Jarl Andre Hubenthal
 */
public class MonitorPage extends AbstractWebPage {
  private static final Logger logger = LoggerFactory.getLogger(MonitorPage.class);

  public boolean useWrapping() {
    return true;
  }

  public void process(
          ParameterParser req,
          Output outputHandler,
          DataSource xapsDataSource,
          DataSource syslogDataSource)
          throws Exception {
    ACS acs = ACSLoader.getXAPS(req.getSession().getId(), xapsDataSource, syslogDataSource);

    if (acs == null) {
      outputHandler.setRedirectTarget(WebConstants.LOGIN_URI);
      return;
    }

    String cmd = req.getParameter("page");
    String baseURL = WebProperties.getInstance().getMonitorLocation();
    if (baseURL == null) {
      baseURL = "http://localhost:8090/monitor/";
    }
    if (!baseURL.endsWith("/")) {
      baseURL += "/";
    }

    String url;
    if (cmd != null) {
      if ("status".equalsIgnoreCase(cmd) || "monitor".equalsIgnoreCase(cmd)) {
        url = baseURL + "?page=status&html=no";
      } else if ("history".equalsIgnoreCase(cmd)) {
        url = baseURL + "?page=history&html=no";
      } else {
        url = baseURL + "?html=no";
      }
    } else {
      url = baseURL + "?html=no";
    }

    String responseBody = getStringFromURL(url, req);

    if (responseBody == null) {
      Map<String, String> root = new HashMap<>();
      root.put("message", "Monitor Service is not running!");
      outputHandler.getTemplateMap().putAll(root);
      responseBody = outputHandler.compileTemplate("/exception.ftl");
    }

    outputHandler.setDirectResponse(responseBody);
  }

  /**
   * Fetches data from the given URL using Apache HttpClient 5.
   *
   * @param url the target URL
   * @param req the parameter parser for source data
   * @return the response body as string or null if failure
   */
  private String getStringFromURL(String url, ParameterParser req) {
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpUriRequestBase request;

      if (url.contains("page=history")) {
        HttpPost post = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<>();
        String limit = req.getParameter("limit");
        String system = req.getParameter("system");
        if (limit != null) params.add(new BasicNameValuePair("limit", limit));
        if (system != null) params.add(new BasicNameValuePair("system", system));

        if (!params.isEmpty()) {
          post.setEntity(new UrlEncodedFormEntity(params));
        }
        request = post;
      } else {
        request = new HttpGet(url);
      }

      return client.execute(request, response -> {
        if (response.getCode() == HttpStatus.SC_OK) {
          return EntityUtils.toString(response.getEntity());
        }
        logger.warn("Monitor server returned status code: {}", response.getCode());
        return null;
      });

    } catch (IOException e) {
      logger.warn("Could not find the monitor server at {}", url, e);
      return null;
    }
  }
}
