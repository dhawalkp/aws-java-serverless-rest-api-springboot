package com.amazon.sa;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import java.util.Optional;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

//import com.amazonaws.xray.proxies.apache.http.HttpClientBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class USCISCaseStatusController {

	private static final Logger log = LoggerFactory.getLogger(USCISCaseStatusController.class);

	@GetMapping("/USCISCaseStatusPoller/case/{caseid}/status")
	public USCISCaseStatus caseStatus(@PathVariable String caseid) {
		InputStream stream;
		String rawResponse, status = "Could not retrieve";
		log.warn("Executing the request");
		System.out.println("Executing the request");

		final String startString = "<strong>Your Current Status:</strong>";
		final String endString = "<span class=\"appointment-sec-show\" tabindex=\"-1\" title=\"View Case Status Full Description\">+</span>";

		// Calling the external HTTP Call via Spring RestTemplate

		/*
		 * try { RestTemplate restTemplate = new RestTemplate(); Quote quote =
		 * restTemplate.getForObject(
		 * "https://egov.uscis.gov/casestatus/mycasestatus.do", Quote.class);
		 * log.warn(quote.toString()); }catch(Exception exp) {
		 * log.warn("Spring HTTP Client failed to call Outgoing REST API");
		 * exp.printStackTrace();
		 * 
		 * }
		 */

		// Using Apache HTTP Client so that XRay Tracing can be enabled using the custom
		// HttpClientBuilder from AWS XRay SDK.
		// I am hitting a bug currently with XRay custom HTTPClientBuilder and opened an
		// issue with XRay SDK Dev team. Temporarily using Apache HTTP Client builder
		// this will cause not to generate sub-segment IDs for outbound calls.
		CloseableHttpResponse response = null;

		// Calling REST API via Apache HTTP Builder
		try {
			CloseableHttpClient httpclient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("https://egov.uscis.gov/casestatus/mycasestatus.do");
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("changeLocale", ""));
			params.add(new BasicNameValuePair("appReceiptNum", caseid));
			params.add(new BasicNameValuePair("initCaseSearch", "CHECK+STATUS"));
			httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			httpPost.addHeader("Host", "egov.uscis.gov");
			httpPost.addHeader("Origin", "https://egov.uscis.gov");
			httpPost.addHeader("Referer", "https://egov.uscis.gov/casestatus/landing.do");
			httpPost.addHeader("Content-Encoding", "gzip, deflate");

			httpPost.setEntity(new UrlEncodedFormEntity(params));

			response = httpclient.execute(httpPost);

			// log.info(response.getEntity().getContentEncoding().toString());
			// log.info(response.getEntity().getContent().toString());
			if (response.getStatusLine().getStatusCode() == 200) {
				stream = response.getEntity().getContent();
				rawResponse = streamToString(stream);
				log.warn(rawResponse);
				System.out.println(rawResponse);
				status = rawResponse.substring(rawResponse.indexOf(startString) + startString.length(),
						rawResponse.indexOf(endString)).trim();
				log.warn("YOur case " + caseid + " status is " + status);
			}

			HttpEntity entity = response.getEntity();
			InputStream inputStream = entity.getContent();
			ObjectMapper mapper = new ObjectMapper();

			EntityUtils.consume(entity);

		} catch (Exception exp) {
			log.warn("XRay HTTP Client failed to call Outgoing REST API");
			System.out.println("XRay HTTP Client failed to call Outgoing REST API");
			exp.printStackTrace();

		}

		finally {

			try {
				Optional<CloseableHttpResponse> optionalResponse = Optional.ofNullable(response);
				if (optionalResponse.isPresent()) {
					response.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return new USCISCaseStatus(caseid, status);
	}

	private static String streamToString(InputStream stream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (IOException e) {
			log.error("Error while streaming to string: {}", e);
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}

		return sb.toString();
	}
}
