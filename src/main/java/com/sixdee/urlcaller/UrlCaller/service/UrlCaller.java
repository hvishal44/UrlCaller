package com.sixdee.urlcaller.UrlCaller.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Service
public class UrlCaller {
	
	private String connectionTimeoutStr=System.getProperty("URL_READ_TIMEOUT");
	private String http_connection_timeoutStr=System.getProperty("HTTP_CONNECTION_TIMEOUT");
	
	
	private int connectionTimeout = connectionTimeoutStr==null?300000:Integer.parseInt(connectionTimeoutStr);
	private int http_connection_timeout = http_connection_timeoutStr==null?300000:Integer.parseInt(http_connection_timeoutStr);
	
	final static Logger logger = LoggerFactory.getLogger(UrlCaller.class);
	static {
		System.setProperty("http.keepAlive", "true");
		System.setProperty("http.maxConnections", "40");
	}
	
	

	public  Map<String, String> sendGetWithHeadersAndQuery(String url, Map<String, Object> queryMap, Map<String, String> headers, String method)
			throws Exception {


		StringBuffer sb = null;
		String query = "";
		for (String key : queryMap.keySet()) {
			if(queryMap.get(key)!=null) {
				String value = (String) queryMap.get(key);
				value = URLEncoder.encode(value);
				query = query + key + "=" + value + "&";
			}
		}
		if(queryMap.size() >0) {
			query = query.substring(0, query.length() - 1);
		}
		URL obj = new URL(url + query);
		logger.info("url obj : " + obj);
		Map<String, String> responseMap = new HashMap<String, String>();
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		try {
		
		for (String key : headers.keySet())
			con.setRequestProperty(key, headers.get(key));

		con.setRequestMethod(method);

		int responseCode = con.getResponseCode();
		responseMap.put("responseCode", Integer.toString(responseCode));
		if (logger.isDebugEnabled())
			logger.debug("[Sending 'GET' request to URL : " + url + "] [Response Code : " + responseCode + "]");

		StringBuffer response = null;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));) {

			response = new StringBuffer();
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}
		if (logger.isDebugEnabled())
			logger.debug(response.toString());
		responseMap.put("response", response.toString());

		} catch (IOException ie) {
			System.out
					.println("Couldnt get the Input stream: can be a timeout or data present in Error Stream");

			String rspStr=null;
			try {
				System.out.println("Trying to get data from Error Stream");
				InputStream es = con.getErrorStream();
				int responseCode = con.getResponseCode();
				responseMap.put("responseCode", Integer.toString(responseCode));
				
				if (es != null) {
					sb = new StringBuffer();
					int i = -1;
					while (es != null && (i = es.read()) != -1) {
						sb.append((char) i);
					}
					rspStr=sb.toString();
					responseMap.put("response", rspStr);
					sb = null;
					es.close();
					es = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Still exception. This can be a read timeout");
				throw ie;
			}
			if (rspStr == null)
				throw new Exception("read/connection time out");
		}
		return responseMap;
	}
	
	/*public  Map<String, String> sendGetWithQuery(String url, Map<String, String> queryMap, String method)
			throws Exception {


		String query = "";
		for (String key : queryMap.keySet()) {
			String value = queryMap.get(key);
			value = URLEncoder.encode(value);
			query = query + key + "=" + value + "&";
		}
		if(queryMap.size() >0) {
			query = query.substring(0, query.length() - 1);
		}
		URL obj = new URL(url + query);
		logger.info("url obj : " + obj);
		Map<String, String> responseMap = new HashMap<String, String>();
		
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod(method);

		int responseCode = con.getResponseCode();
		responseMap.put("responseCode", Integer.toString(responseCode));
		if (logger.isDebugEnabled())
			logger.debug("[Sending 'GET' request to URL : " + url + "] [Response Code : " + responseCode + "]");

		StringBuffer response = null;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));) {

			response = new StringBuffer();
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}
		if (logger.isDebugEnabled())
			logger.debug(response.toString());
		responseMap.put("response", response.toString());

		return responseMap;
	}*/
	

	@SuppressWarnings("unlikely-arg-type")
	public  Map<String, String> goPostWithHeaders(String urlPath, String reqStr, Map<String, String> headers,
			String method) throws Exception {
		String rspStr = null;
		OutputStreamWriter out = null;
		InputStream in = null;
		HttpURLConnection connection = null;
		StringBuffer sb = null;
		Map<String, String> responseMap = new HashMap<String, String>();
		boolean isOutput = true;

		try {
			// if(logger.isDebugEnabled())logger.debug(urlPath + "->" +
			// reqStr);
			URL url = new URL(urlPath.trim());
			connection = (HttpURLConnection) url.openConnection();
			if (method.equalsIgnoreCase("GET"))
				isOutput = false;
			else
				isOutput = true;
			connection.setDoOutput(isOutput);
			System.out.println("read timeout : "+connectionTimeout);
			System.out.println("connect timeout : "+http_connection_timeout);
			connection.setConnectTimeout(http_connection_timeout);
			connection.setReadTimeout(connectionTimeout);
			connection.setRequestMethod(method);
			for (String key : headers.keySet())
				connection.setRequestProperty(key, headers.get(key));
			if (isOutput) {
				connection.setDoOutput(true);
				try {
					out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
					out.write(reqStr);
					out.flush();
					out.close();
				} catch (IOException ie) {
					ie.printStackTrace();
					logger.debug("timeout exception");
					throw new IOException("timeout exception ");
				}
			}
			int i = -1;
			logger.info("responseCode : [" + connection.getResponseCode() + "]");
			responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));
			if (204 != (connection.getResponseCode())) {
				try {
					in = connection.getInputStream();
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					sb = new StringBuffer();
					while ((i = bufferedReader.read()) != -1) {
						sb.append((char) i);
					}
					rspStr = sb.toString();
					responseMap.put("response", rspStr);
					in.close();
					bufferedReader.close();
					return responseMap;
				} catch (IOException ie) {
					System.out
							.println("Couldnt get the Input stream: can be a timeout or data present in Error Stream");

					try {
						System.out.println("Trying to get data from Error Stream");
						InputStream es = connection.getErrorStream();
						if (es != null) {
							sb = new StringBuffer();
							i = -1;
							while (es != null && (i = es.read()) != -1) {
								sb.append((char) i);
							}
							rspStr = sb.toString();
							responseMap.put("response", rspStr);
							sb = null;
							es.close();
							es = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Still exception. This can be a read timeout");
						throw ie;
					}
					if (rspStr == null)
						throw new Exception("read/connection time out");
				}
			}
		} catch (SocketTimeoutException se) {
			logger.error("Socket Timeout Exception : [" + se.getMessage() + " ]", se);
			se.printStackTrace();
			throw se;
		} catch (Exception e) {
			responseMap.put("response", e.getMessage());
			//responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));
			logger.error("Exception occured : [" + e.getMessage() + " ]", e);
			throw e;
		} finally {
			if (out != null)
				out.close();
			out = null;
			if (in != null)
				in.close();
			in = null;
		}
		return responseMap;
	}
	
//	Getting Content-Type for INS from system.properties. So if any change in content-Type while testing with innovative system, need to change in system.properties
	@SuppressWarnings("unlikely-arg-type")
	public  Map<String, String> goPostWithHeadersForINS(String urlPath, String reqStr, Map<String, String> headers,
			String method) throws Exception {
		String rspStr = null;
		OutputStreamWriter out = null;
		InputStream in = null;
		HttpURLConnection connection = null;
		StringBuffer sb = null;
		Map<String, String> responseMap = new HashMap<String, String>();
		boolean isOutput = true;

		try {
			// if(logger.isDebugEnabled())logger.debug(urlPath + "->" +
			// reqStr);
			URL url = new URL(urlPath.trim());
			connection = (HttpURLConnection) url.openConnection();
			if (method.equalsIgnoreCase("GET"))
				isOutput = false;
			else
				isOutput = true;
			connection.setDoOutput(isOutput);
			connection.setConnectTimeout(http_connection_timeout);
			connection.setReadTimeout(connectionTimeout);
			connection.setRequestMethod(method);
			connection.setRequestProperty("Content-Type", System.getProperty("ContentTypeForIns"));
			for (String key : headers.keySet())
				connection.setRequestProperty(key, headers.get(key));
			if (isOutput) {
				connection.setDoOutput(true);
				try {
					out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
					out.write(reqStr);
					out.flush();
					out.close();
				} catch (IOException ie) {
					ie.printStackTrace();
					logger.debug("timeout exception");
					throw new IOException("timeout exception ");
				}
			}
			int i = -1;
			logger.info("responseCode : [" + connection.getResponseCode() + "]");
			responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));
			if (204 != (connection.getResponseCode())) {
				try {
					in = connection.getInputStream();
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					sb = new StringBuffer();
					while ((i = bufferedReader.read()) != -1) {
						sb.append((char) i);
					}
					rspStr = sb.toString();
					responseMap.put("response", rspStr);
					in.close();
					bufferedReader.close();
					return responseMap;
				} catch (IOException ie) {
					System.out
							.println("Couldnt get the Input stream: can be a timeout or data present in Error Stream");

					try {
						System.out.println("Trying to get data from Error Stream");
						InputStream es = connection.getErrorStream();
						if (es != null) {
							sb = new StringBuffer();
							i = -1;
							while (es != null && (i = es.read()) != -1) {
								sb.append((char) i);
							}
							rspStr = sb.toString();
							responseMap.put("response", rspStr);
							sb = null;
							es.close();
							es = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Still exception. This can be a read timeout");
						throw ie;
					}
					if (rspStr == null)
						throw new Exception("read/connection time out");
				}
			}
		} catch (SocketTimeoutException se) {
			logger.error("Socket Timeout Exception : [" + se.getMessage() + " ]", se);
			se.printStackTrace();
			throw se;
		} catch (Exception e) {
			responseMap.put("response", e.getMessage());
			//responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));
			logger.error("Exception occured : [" + e.getMessage() + " ]", e);
			throw e;
		} finally {
			if (out != null)
				out.close();
			out = null;
			if (in != null)
				in.close();
			in = null;
		}
		return responseMap;
	}
	
	public Map<String, Object> goPostWithHeader(String urlPath, String reqStr, Map<String, String> headers,
			boolean isOutput) throws Exception {

		String rspStr = null;
		OutputStreamWriter out = null;
		InputStream in = null;
		HttpURLConnection connection = null;
		// URLConnection connection = new URL(url).openConnection();
		StringBuffer sb = null;
		Map<String, Object> responseMap = new HashMap<String, Object>();

		try {
			// if(logger.isDebugEnabled())logger.debug(urlPath + "->" +
			// reqStr);
			URL url = new URL(urlPath.trim());

			// First set the default cookie manager.
			CookieManager cookieManager = new CookieManager();
			// setDefault() method sets the system-wide cookie handler
			CookieHandler.setDefault(cookieManager);

			HttpURLConnection.setFollowRedirects(false);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setConnectTimeout(http_connection_timeout);
			connection.setReadTimeout(connectionTimeout);
			for (String key : headers.keySet())
				connection.setRequestProperty(key, headers.get(key));
			if (isOutput) {
				connection.setDoOutput(true);
				try {
					out = new OutputStreamWriter(connection.getOutputStream(), "utf-8");
					out.write(reqStr);
					out.flush();
					out.close();
				} catch (IOException ie) {
					ie.printStackTrace();
					logger.debug("timeout exception");
					throw new Exception("timeout exception ");
				}
			}
			int i = -1;
			logger.debug("responseCode : [" + connection.getResponseCode() + "]");

			logger.info("responseCode : [" + connection.getResponseCode() + "]");

			responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));

			if (connection.getHeaderField("Location") != null && !"".equals(connection.getHeaderField("Location"))) {
				responseMap.put("Location", connection.getHeaderField("Location"));
			}

			try {
				in = connection.getInputStream();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "utf-8"));
				sb = new StringBuffer();
				while ((i = bufferedReader.read()) != -1) {
					sb.append((char) i);
				}
				rspStr = sb.toString();
				responseMap.put("response", rspStr);
				in.close();
				bufferedReader.close();
			} catch (IOException ie) {
				System.out.println("Couldnt get the Input stream: can be a timeout or data present in Error Stream");

				try {
					System.out.println("Trying to get data from Error Stream");
					InputStream es = connection.getErrorStream();
					if (es != null) {
						sb = new StringBuffer();
						i = -1;
						while (es != null && (i = es.read()) != -1) {
							sb.append((char) i);
						}
						rspStr = sb.toString();
						responseMap.put("response", rspStr);
						sb = null;
						es.close();
						es = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Still exception. This can be a read timeout");
					throw ie;
				}
				if (rspStr == null)
					throw new Exception("read/connection time out");
			}

		} catch (SocketTimeoutException se) {
			logger.error("Socket Timeout Exception : [" + se.getMessage() + " ]", se);
			se.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception occured : [" + e.getMessage() + " ]", e);
			throw e;
		} finally {
			if (out != null)
				out.close();
			out = null;
			if (in != null)
				in.close();
			in = null;
		}
		return responseMap;
	}
	
	public  Map<String, Object> goPostWithHeader(String urlPath, String reqStr, Map<String, String> headers,
			String method) throws Exception {
		String rspStr = null;
		OutputStreamWriter out = null;
		InputStream in = null;
		HttpURLConnection connection = null;
		StringBuffer sb = null;
		Map<String, Object> responseMap = new HashMap<String, Object>();
		boolean isOutput = true;

		try {
			// if(logger.isDebugEnabled())logger.debug(urlPath + "->" +
			// reqStr);
			URL url = new URL(urlPath.trim());
			connection = (HttpURLConnection) url.openConnection();
			if (method.equalsIgnoreCase("GET"))
				isOutput = false;
			else
				isOutput = true;
			connection.setDoOutput(isOutput);
			connection.setConnectTimeout(http_connection_timeout);
			connection.setReadTimeout(connectionTimeout);
			connection.setRequestMethod(method);
			for (String key : headers.keySet())
				connection.setRequestProperty(key, headers.get(key));
			if (isOutput) {
				connection.setDoOutput(true);
				try {
					out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
					out.write(reqStr);
					out.flush();
					out.close();
				} catch (IOException ie) {
					ie.printStackTrace();
					logger.debug("timeout exception");
					throw new IOException("timeout exception ");
				}
			}
			int i = -1;
			logger.info("responseCode : [" + connection.getResponseCode() + "]");
			responseMap.put("responseCode", Integer.toString(connection.getResponseCode()));
			if (204 != (connection.getResponseCode())) {
				try {
					in = connection.getInputStream();
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
					sb = new StringBuffer();
					while ((i = bufferedReader.read()) != -1) {
						sb.append((char) i);
					}
					rspStr = sb.toString();
					responseMap.put("response", rspStr);
					in.close();
					bufferedReader.close();
					return responseMap;
				} catch (IOException ie) {
					System.out
							.println("Couldnt get the Input stream: can be a timeout or data present in Error Stream");

					try {
						System.out.println("Trying to get data from Error Stream");
						InputStream es = connection.getErrorStream();
						if (es != null) {
							sb = new StringBuffer();
							i = -1;
							while (es != null && (i = es.read()) != -1) {
								sb.append((char) i);
							}
							rspStr = sb.toString();
							responseMap.put("response", rspStr);
							sb = null;
							es.close();
							es = null;
						}
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("Still exception. This can be a read timeout");
						throw e;
					}
					if (rspStr == null)
						throw new Exception("read/connection time out");
				}
			}
		} catch (SocketTimeoutException se) {
			logger.error("Socket Timeout Exception : [" + se.getMessage() + " ]", se);
			se.printStackTrace();
			throw se;
		} catch (Exception e) {
			logger.error("Exception occured : [" + e.getMessage() + " ]", e);
			throw e;
		} finally {
			if (out != null)
				out.close();
			out = null;
			if (in != null)
				in.close();
			in = null;
		}
		return responseMap;
	}

	public  Map<String, String> sendGetReturnsMap(String url , String method)
			throws Exception {

		URL obj = new URL(url);
		Map<String, String> responseMap = new HashMap<String, String>();
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		con.setRequestMethod(method);

		int responseCode = con.getResponseCode();
		responseMap.put("responseCode", Integer.toString(responseCode));
		if (logger.isDebugEnabled())
			logger.debug("[Sending 'GET' request to URL : " + url + "] [Response Code : " + responseCode + "]");

		StringBuffer response = null;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));) {

			response = new StringBuffer();
			String inputLine = null;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}
		if (logger.isDebugEnabled())
			logger.debug(response.toString());
		responseMap.put("response", response.toString());

		return responseMap;
	}

	public void sendDelete(String url) throws RestClientException{
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();

		clientHttpRequestFactory.setConnectTimeout(http_connection_timeout);

		clientHttpRequestFactory.setReadTimeout(connectionTimeout);
		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);

		restTemplate.delete(url);
		
//		URL urlObj = new URL(url);
//		HttpURLConnection httpCon = (HttpURLConnection) urlObj.openConnection();
//		httpCon.setDoOutput(true);
//		httpCon.setRequestProperty(
//		    "Content-Type", "application/x-www-form-urlencoded" );
//		httpCon.setRequestMethod("DELETE");
//		httpCon.connect();

	}

}
