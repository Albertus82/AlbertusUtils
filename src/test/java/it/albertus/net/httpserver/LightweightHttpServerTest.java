package it.albertus.net.httpserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import it.albertus.net.httpserver.annotation.Path;
import it.albertus.net.httpserver.config.HttpServerDefaultConfig;
import it.albertus.net.httpserver.config.IHttpServerConfig;
import it.albertus.net.httpserver.config.SingleUserAuthenticatorDefaultConfig;
import it.albertus.util.IOUtils;
import it.albertus.util.logging.LoggerFactory;
import it.albertus.util.logging.LoggingSupport;

@SuppressWarnings("restriction")
public class LightweightHttpServerTest {

	@Path(HANDLER_PATH_DISABLED)
	private static class DisabledHandler extends BaseHttpHandler {
		public DisabledHandler(final IHttpServerConfig config) {
			super(config);
		}

		@Override
		protected void doGet(final HttpExchange exchange) throws IOException {
			sendResponse(exchange, loremSmallTxt.getBytes());
		}
	}

	@Path(HANDLER_PATH_PARAMS)
	private static class RequestParameterHandler extends BaseHttpHandler {
		public RequestParameterHandler(final IHttpServerConfig config) {
			super(config);
		}

		@Override
		protected void service(HttpExchange exchange) throws IOException {
			for (final Filter filter : exchange.getHttpContext().getFilters()) {
				log.fine(filter.description());
			}
			super.service(exchange);
		}

		@Override
		protected void doGet(final HttpExchange exchange) throws IOException {
			log.log(Level.INFO, exchange.getRequestHeaders().entrySet().toString());
			RequestParameterExtractor r = new RequestParameterExtractor(exchange);
			Assert.assertEquals(4, r.getParameterMap().size());

			final Map<String, String[]> params = new TreeMap<String, String[]>(); // sorted
			params.putAll(r.getParameterMap());

			final String queryString = buildQueryString(params);
			sendResponse(exchange, queryString.getBytes());
		}

		@Override
		protected void doPost(final HttpExchange exchange) throws IOException {
			doGet(exchange);
		}

		@Override
		protected void setContentLanguageHeader(final HttpExchange exchange) {
			setContentLanguageHeader(exchange, "de");
		}
	}

	private static final Logger log = LoggerFactory.getLogger(LightweightHttpServerTest.class);

	private static final List<SocketTimeoutException> exceptions = new ArrayList<SocketTimeoutException>();

	private static final String JKS = "/u3+7QAAAAIAAAABAAAAAQAFYWxpYXMAAAFbwGV5jwAABQIwggT+MA4GCisGAQQBKgIRAQEFAASCBOpsC1WrIp+Z21CyHK/JU2NGDnYPjz9KR+/ENVVueMN5EnRqpznB+iq8fBgRluppyY78c+YQCSTngc39f3ULBuTaGKisWFEpb1Iy5BlyB4ymFGagd32ITrAp9oB8X2rAtkUbM7RggcdqvwNj95paJJhK/n0swAeyPIhQLaRAo+ZCVk1PwFw3gq9fpMJVyZiBufXYtgXZlTr/RLiNJs+YTp49XDanfOhdUJnnU2Zns4aesXQryXKZBcjWIfwKL87iQZ1hi3V9pWFVKgRhQbtQG3NCK7vGxZttT1aclZ9+GVLhQcWYn0m4Y7sPTSBspupDcUtxRWOxxzfX8L2jZW9/Hr/Awccm9oXkjOb3v+x05HH9tOVDNGY0hSjnDM0lf3qjk+UKdqNUl+ThmxEIbmykW9sOmxJnWdwVJJUtANypGvVOYAs6avgHhK/WTP5MXc98LCIIckIZum/rczKqjMJQdS6jkZSzTvBUwF7dA0e5+TMn1dI2KEfhGcRFIlqvcy6evYjJHgmXX5n9GD3axTfAQE5OW7srRQXM7rxQ2Lvpr2VVjXH3f7w0uiYFVA5chGlYNXz65E1mQUhvj3rTRX+bfkorncRp+DWzvbRGrLvhPaLNWhP7AHahqm6X4Xu6aZuB7D2ftCSeUXVn4j1D3S4sLQfGPUgigsGGiDtJZDFUYzAYnlQPeTCYec5Ghgcx+VfEmJZNuvCzMt5ubt7OQxGzcKmzoOr5epIb53sXV2PPdMAj5nOQ31m0a77eUCRnLeQ8p8ite9V/x0mfqCCjbVSu5Zb1yYjKlsM1pUZ6nVNHQLvJQjT989iHz5ubTjYOF8838b8lSx4lXi3scy2Z8dYo9THufOCft5b5x9BxBKKZlTosXFDKNrixYSt0IZVGV6L+E2fep52VDrljKiY+cnHid4B1xQFjcTndmNgT0fDpY6WACTkx/ufiyjMM9Y2jmvQFwy2ZSjP821Fh5fOXE24Cgqo25FINXiVuyZ8u+GA/o7JPgjq07QmkJyaH9IZdjoIsbla60oaTym3vnU2hzWO6IpvecKor702ANA+j5/8nltsegTnLRaQO925415kd8wHECUfHLWmICS2q2lY45v74gTXIBeoAG1Syk3SLWK5UHF6mGJvuwJI8YhPKSTBUPeN+AgDqfZsH4LgSlbT9QuKoQ5OOb2bWCTlzeIRwbNl383AlGehVcam/vRBr9rNK0MhudKEHaLHUviZwQRuY11LfAGpuKabv1RzBTut/wQ+WGnPFmY4ucnhjTm1qolYk8LYQ666GDbz76KuYv9AR18/bKTTdZrxXg+nbaPj1aOO7uFtLnrjTPp3WYfHN0em+NfSfulr7TpcZrQXeVevzbDbfMeA0+kqt/YvWv4iQIxuldzi9D93ml6K/Vzd/8cPQny4yEYsbgpJdYxpRSW6a25yjIqjgP+ZSVwjt1hryaNNdUryypue8M7W9GJzQzRiqk8ZjiX28mU2cZmUdXl8wGeefCNyGN6snv249MIPvcMOShYDYtTBsq9ffYZ0Y76OAmipxrPh9U0uQdgbMRPIf/vycHb+jKLkvm3NEU0uW0zeBSFMzSF+2gnCMhtDDektpijiGhuXXtHwzS07dmcoMJbJOJq3cm/ldGBzY5BwUz1yV3klK+1ChWOup/UQRI3Cfv9p57NREjFi56a7zbaYDAAAAAQAFWC41MDkAAAN7MIIDdzCCAl+gAwIBAgIEdRzxATANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE3MDQzMDE5NDUyNloXDTM3MDExNTE5NDUyNlowbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALo8nZUvPMA6aY+xgmNb4d/lLigFMUYKY5x0Q+GT7I3es/jt+//ow4CbgrXFlVEBj7LRxo9kz82ayCQbHrIG/b5wgnKFvnTJfIfutSLjxxGADuVjv3fK6AG5hRLV3nEb/88ETegUVZvKCHZCE7UatVNzj840olYz1IKEo8cpfx1TdDBkp15NymqFbjPfABx1zfGT4o2gsQReCQCeprZxqD8pmAr1K5z+yZe90H7GxbQ3/KJJfUV9xzSQyFoeiZm7QRzFesxUXGouTbMyBa6Cpir3DHVCG3GCTZPrFuWzYSFNSqqq3oINHCYeNmkbksq5cexqoJtT9pmREWjUnYvq/YkCAwEAAaMhMB8wHQYDVR0OBBYEFEBvbH5HdQjsLqFeQXdO3dGquPqsMA0GCSqGSIb3DQEBCwUAA4IBAQCv/ZfidiJaM5nid15L68PY/rXU1lbfrs/Iq3ha2PMZL45wLNKUNZWB95Gc1ToCYy/1ci59pTfhNUEZhKqnRIulzE+QAW7JNj5Vd/QaHz64TJBbtyVzU3/bVUhUUEuMmLRicLhnePVzpYnROnbikwvgVuzS0d6YM20mLcDoJaAt3q63vDwiVssr/u3svyeAT35tDT/By7VvCT2bPHO7Ee1LILtlv/4TtA365BQZ1nHmxY7psx8VBLh1Q190NOj1Dc6irPiW+4v/ZDwzsy9xZeu0Z1kq9pcJjSee91HzuRsy5hpDdPqRPEw9EeQMPlh+afz8rdcnycUFbFjrjGV4AG/+r7OckbqtRKyka+zTk0XG6xYch84=";
	private static final String USERNAME = "test";
	// private static final String PASSWORD = "TESTtest12345";
	private static final String PASSWORD_SHA1 = "92f1a57051141e9d24396bc42ae43b6500d13f8b";

	private static final String REALM = "Test Realm";
	private static final String CREDENTIALS_BASE64 = "dGVzdDpURVNUdGVzdDEyMzQ1";
	private static final String HANDLER_PATH_TXT = "/loremIpsum.txt";

	private static final String HANDLER_PATH_DISABLED = "/disabled.txt";
	private static final String HANDLER_PATH_PARAMS = "/params.txt";
	private static final String loremSmallTxt = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

	private static final String loremSmallMd5 = "\"db89bb5ceab87f9c0fcc2ab36c189c2c\"";
	private static final String loremLargeMd5 = "\"677560ee0c55b61d73bf47e79835f83a\"";
	private static final Authenticator singleUserAuthenticator = new HttpServerAuthenticator(new SingleUserAuthenticatorDefaultConfig() {
		@Override
		public char[] getPassword() {
			return PASSWORD_SHA1.toCharArray();
		}

		@Override
		public String getPasswordHashAlgorithm() {
			return "SHA-1";
		}

		@Override
		public String getRealm() {
			return REALM;
		}

		@Override
		public String getUsername() {
			return USERNAME;
		}
	});

	private static boolean sslEnabled = false;
	private static int port = 10000;
	private static Authenticator authenticator;

	private static LightweightHttpServer server;

	private static File certificate;

	private static String buildQueryString(final Map<String, String[]> params) {
		final StringBuilder queryString = new StringBuilder();
		for (final Entry<String, String[]> e : params.entrySet()) {
			for (final String value : e.getValue()) {
				queryString.append('&');
				queryString.append(e.getKey()).append('=').append(value);
			}
		}
		return queryString.substring(1);
	}

	@AfterClass
	public static void destroy() {
		if (!certificate.delete()) {
			certificate.deleteOnExit();
		}
		LoggingSupport.setRootLevel(Level.INFO);
		if (exceptions.size() > 0) {
			log.log(Level.WARNING, "Total errors count: {0}.", exceptions.size());
		}
	}

	@BeforeClass
	public static void init() throws InterruptedException, IOException {
		LoggingSupport.setRootLevel(Level.FINE);
		certificate = File.createTempFile("cert-", ".jks");
		log.info(certificate.toString());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(certificate);
			fos.write(DatatypeConverter.parseBase64Binary(JKS));
		}
		finally {
			IOUtils.closeQuietly(fos);
		}

		final IHttpServerConfig configuration = new HttpServerDefaultConfig() {
			@Override
			public Authenticator getAuthenticator() {
				return authenticator;
			}

			@Override
			public BaseHttpHandler[] getHandlers() {
				final BaseHttpHandler h1 = new BaseHttpHandler(this) {
					@Override
					protected void doGet(final HttpExchange exchange) throws IOException, HttpException {
						sendResponse(exchange, loremSmallTxt.getBytes());
					}
				};
				h1.setPath(HANDLER_PATH_TXT);

				final BaseHttpHandler h2 = new DisabledHandler(this);
				h2.setEnabled(false);

				final BaseHttpHandler h3 = new RequestParameterHandler(this);

				final FilesHandler h4 = new FilesHandler(this, certificate.getParentFile().getPath(), "/files/");
				h4.setAttachment(true);

				final ResourcesHandler h5 = new ResourcesHandler(this, getClass().getPackage(), "/resources/");
				h5.setAttachment(true);
				h5.setCacheControl("max-age=86400");

				final ResourcesHandler h6 = new ResourcesHandler(this, (Package) null, "/root/");

				final FilesHandler h7 = new FilesHandler(this, certificate.getParentFile().getPath() + "/backtracking", "/backtracking/");

				return new BaseHttpHandler[] { h1, h2, h3, h4, h5, h6, h7 };
			}

			@Override
			public char[] getKeyPass() {
				return "keypass".toCharArray();
			}

			@Override
			public String getKeyStoreFileName() {
				return certificate.getPath();
			}

			@Override
			public int getPort() {
				return port;
			}

			@Override
			public char[] getStorePass() {
				return "storepass".toCharArray();
			}

			@Override
			public boolean isSslEnabled() {
				return sslEnabled;
			}

			@Override
			public boolean isTraceMethodEnabled() {
				return true;
			}
		};
		server = new LightweightHttpServer(configuration);
	}

	private static void startServer() throws InterruptedException {
		server.start();

		final int retryPeriod = 100; // ms
		final int timeout = 3500; // ms
		int time = 0;
		do {
			try {
				TimeUnit.MILLISECONDS.sleep(time += retryPeriod);
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
		}
		while (!server.isRunning() && time < timeout);
	}

	@Before
	public void changePort() {
		port++;
	}

	private void configureSsl() throws NoSuchAlgorithmException, KeyManagementException {
		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new DummyTrustManager() }, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new DummyHostnameVerifier("localhost"));
	}

	@Test
	public void makeGetRequestWithParams() throws IOException, InterruptedException {
		try {
			final Map<String, String[]> params = new TreeMap<String, String[]>(); // sorted
			params.put("param1", new String[] { "qwertyuiop" });
			params.put("param2", new String[] { "asdfghjkl" });
			params.put("param3", new String[] { "zxcvbnm" });
			params.put("multi", new String[] { "value1", "value2", "value3" });
			final String queryString = buildQueryString(params);

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_PARAMS + '?' + queryString);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNotNull(connection.getHeaderField("Etag"));
			Assert.assertEquals(queryString.length(), connection.getContentLength());
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, 256);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(queryString, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makePostRequestWithParams() throws IOException, InterruptedException {
		try {
			final Map<String, String[]> params = new TreeMap<String, String[]>(); // sorted
			params.put("param1", new String[] { "qwertyuiop" });
			params.put("param2", new String[] { "asdfghjkl" });
			params.put("param3", new String[] { "zxcvbnm" });
			params.put("multi", new String[] { "value1", "value2", "value3" });
			final String queryString = buildQueryString(params);

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_PARAMS);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.POST.toUpperCase());
			connection.setDoOutput(true);
			connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.addRequestProperty("Content-Length", Integer.toString(queryString.length() - 1));
			connection.getOutputStream().write(queryString.getBytes());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Etag"));
			Assert.assertEquals(queryString.length(), connection.getContentLength());
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, 256);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(queryString, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makePostRequestWithGzippedBody() throws IOException, InterruptedException {
		try {
			final Map<String, String[]> params = new TreeMap<String, String[]>(); // sorted
			params.put("param1", new String[] { "qwertyuiop" });
			params.put("param2", new String[] { "asdfghjkl" });
			params.put("param3", new String[] { "zxcvbnm" });
			params.put("multi", new String[] { "value1", "value2", "value3" });
			final String queryString = buildQueryString(params);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = null;
			try {
				gzos = new GZIPOutputStream(baos);
				gzos.write(queryString.getBytes());
			}
			finally {
				gzos.close();
			}
			final byte[] compressedQueryString = baos.toByteArray();

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_PARAMS);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.POST.toUpperCase());
			connection.setDoOutput(true);
			connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.addRequestProperty("Content-Length", Integer.toString(compressedQueryString.length - 1));
			connection.addRequestProperty("Content-Encoding", "gzip");
			connection.getOutputStream().write(compressedQueryString);
			log.log(Level.INFO, "Compressed bytes POSTed: ===>{0}<=== ({1})", new String[] { new String(compressedQueryString), DatatypeConverter.printHexBinary(compressedQueryString) });
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Etag"));
			Assert.assertEquals("de", connection.getHeaderField("content-language"));
			Assert.assertEquals(queryString.length(), connection.getContentLength());
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, 256);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(queryString, os.toString());
			log.log(Level.INFO, "Bytes received: ===>{0}<===", os);
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200GzipLargeStaticFile() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File temp = null;
			try {
				tis = getClass().getResourceAsStream("lorem-lg.txt");
				temp = File.createTempFile("lorem-", ".txt");
				tos = new FileOutputStream(temp);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + temp.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("eTag"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("attachment; filename=\"" + temp.getName() + "\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = new GZIPInputStream(connection.getInputStream());
				IOUtils.copy(is, os, 1024);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(temp.length(), os.size());
			if (!temp.delete()) {
				temp.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200GzipLargeStaticFile() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File temp = null;
			try {
				tis = getClass().getResourceAsStream("lorem-lg.txt");
				temp = File.createTempFile("lorem-", ".txt");
				tos = new FileOutputStream(temp);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + temp.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("eTag"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("attachment; filename=\"" + temp.getName() + "\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, 1024);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
			if (!temp.delete()) {
				temp.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200GzipLargeStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-lg.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("ETAG"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-lg.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = new GZIPInputStream(connection.getInputStream());
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(1082518, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200GzipSmallStaticFile() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + certificate.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertTrue(connection.getContentLength() > 0);
			Assert.assertNotNull(connection.getHeaderField("Etag"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertEquals("attachment; filename=\"" + certificate.getName() + "\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = new GZIPInputStream(connection.getInputStream());
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(certificate.length(), os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200GzipSmallStaticFile() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + certificate.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertTrue(connection.getContentLength() > 0);
			Assert.assertNotNull(connection.getHeaderField("Etag"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertEquals("attachment; filename=\"" + certificate.getName() + "\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200GzipSmallStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-sm.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertTrue(connection.getContentLength() > 0);
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-sm.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = new GZIPInputStream(connection.getInputStream());
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200GzipSmallStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-sm.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertTrue(connection.getContentLength() > 0);
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-sm.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200Https() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		try {
			authenticator = null;
			sslEnabled = true;
			startServer();
			configureSsl();
			final URL url = new URL("https://localhost:" + port + HANDLER_PATH_TXT);
			final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200Https() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		try {
			authenticator = null;
			sslEnabled = true;
			startServer();
			configureSsl();
			final URL url = new URL("https://localhost:" + port + HANDLER_PATH_TXT);
			final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200LargeStaticFile() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File temp = null;
			try {
				tis = getClass().getResourceAsStream("lorem-lg.txt");
				temp = File.createTempFile("lorem-", ".txt");
				tos = new FileOutputStream(temp);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + temp.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("eTag"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("attachment; filename=\"" + temp.getName() + "\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(temp.length(), os.size());
			if (!temp.delete()) {
				temp.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200LargeStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-lg.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("ETAG"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-lg.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(1082518, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead200LargeStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-lg.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("ETAG"));
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-lg.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200SmallStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-sm.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("If-None-Match", "1qaz2wsx3edc"); // invalid Etag
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertEquals("attachment; filename=\"lorem-sm.txt\"", connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest200RootSmallStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/root/root-res.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("If-None-Match", "1qaz2wsx3edc"); // invalid Etag
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding")); // not chunked
			Assert.assertNull(connection.getHeaderField("cache-control"));
			Assert.assertNull(connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest304() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("If-None-Match", loremSmallMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			Assert.assertNull(connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(true, os.toString().isEmpty());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest304GzipLargeStaticFile() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File temp = null;
			try {
				tis = getClass().getResourceAsStream("lorem-lg.txt");
				temp = File.createTempFile("lorem-", ".txt");
				tos = new FileOutputStream(temp);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + temp.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			connection.addRequestProperty("If-None-Match", loremLargeMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("eTag"));
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			Assert.assertNull(connection.getHeaderField("Content-Disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
			if (!temp.delete()) {
				temp.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequestHead304GzipLargeStaticFile() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File temp = null;
			try {
				tis = getClass().getResourceAsStream("lorem-lg.txt");
				temp = File.createTempFile("lorem-", ".txt");
				tos = new FileOutputStream(temp);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + temp.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Accept-Encoding", "gzip");
			connection.addRequestProperty("If-None-Match", loremLargeMd5);
			connection.setRequestMethod(HttpMethod.HEAD.toUpperCase());
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("eTag"));
			Assert.assertNull(connection.getHeaderField("Content-Disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
			if (!temp.delete()) {
				temp.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest304GzipLargeStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-lg.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestProperty("Accept-Encoding", "gzip");
			connection.addRequestProperty("If-None-Match", loremLargeMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremLargeMd5, connection.getHeaderField("ETAG"));
			Assert.assertNull(connection.getContentType());
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertNull(connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest304SmallStaticFile() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/files/" + certificate.getName());
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			final String etag = new BaseHttpHandler(null) {}.generateEtag(certificate);
			connection.addRequestProperty("If-None-Match", etag);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			Assert.assertEquals(etag, connection.getHeaderField("Etag"));
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			Assert.assertNull(connection.getHeaderField("Content-disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest304SmallStaticResource() throws InterruptedException, IOException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/resources/lorem-sm.txt");
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("If-None-Match", loremSmallMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertNull(connection.getContentType());
			Assert.assertNull(connection.getHeaderField("Transfer-Encoding"));
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			Assert.assertEquals("max-age=86400", connection.getHeaderField("cache-control"));
			Assert.assertNull(connection.getHeaderField("Content-Disposition"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest401() throws IOException, InterruptedException {
		try {
			authenticator = singleUserAuthenticator;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(401, connection.getResponseCode());
			connection.disconnect();
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest403() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_DISABLED);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(403, connection.getResponseCode());
			Assert.assertEquals("403 " + HttpStatusCodes.getMap().get(403), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			connection.disconnect();
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest404() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + "/qwertyuiop");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(404, connection.getResponseCode());
			connection.disconnect();
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequest404Https() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		try {
			authenticator = null;
			sslEnabled = true;
			startServer();
			configureSsl();
			final URL url = new URL("https://localhost:" + port + "/qwertyuiop");
			final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(404, connection.getResponseCode());
			connection.disconnect();
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequests200Auth() throws IOException, InterruptedException {
		try {
			authenticator = singleUserAuthenticator;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(401, connection.getResponseCode());
			Assert.assertEquals("Basic realm=\"" + REALM + '"', connection.getHeaderField("WWW-Authenticate"));
			connection.disconnect();
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Authorization", "Basic " + CREDENTIALS_BASE64);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequests200HttpsAuth() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		try {
			authenticator = singleUserAuthenticator;
			sslEnabled = true;
			startServer();
			configureSsl();
			final URL url = new URL("https://localhost:" + port + HANDLER_PATH_TXT);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(401, connection.getResponseCode());
			Assert.assertEquals("Basic realm=\"" + REALM + '"', connection.getHeaderField("WWW-Authenticate"));
			connection.disconnect();
			connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Authorization", "Basic " + CREDENTIALS_BASE64);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequests304Auth() throws IOException, InterruptedException {
		try {
			authenticator = singleUserAuthenticator;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_TXT);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(401, connection.getResponseCode());
			Assert.assertEquals("Basic realm=\"" + REALM + '"', connection.getHeaderField("WWW-Authenticate"));
			connection.disconnect();
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Authorization", "Basic " + CREDENTIALS_BASE64);
			connection.addRequestProperty("If-None-Match", loremSmallMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(true, os.toString().isEmpty());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeRequests304HttpsAuth() throws IOException, InterruptedException, NoSuchAlgorithmException, KeyManagementException {
		try {
			authenticator = singleUserAuthenticator;
			sslEnabled = true;
			startServer();
			configureSsl();
			final URL url = new URL("https://localhost:" + port + HANDLER_PATH_TXT);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(401, connection.getResponseCode());
			Assert.assertEquals("Basic realm=\"" + REALM + '"', connection.getHeaderField("WWW-Authenticate"));
			connection.disconnect();
			connection = (HttpsURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.addRequestProperty("Authorization", "Basic " + CREDENTIALS_BASE64);
			connection.addRequestProperty("If-None-Match", loremSmallMd5);
			Assert.assertEquals(304, connection.getResponseCode());
			Assert.assertEquals("304 " + HttpStatusCodes.getMap().get(304), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("Etag"));
			if (connection.getContentLength() != -1) {
				log.log(Level.WARNING, "HTTP 304 responses should not set a content length!");
			}
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertTrue(os.toString().isEmpty());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeOptionsRequest() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_PARAMS);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.OPTIONS.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			//		Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			final String allow = connection.getHeaderField("Allow").toLowerCase(Locale.ROOT);
			Assert.assertTrue(allow.contains(HttpMethod.GET) && allow.contains(HttpMethod.HEAD) && allow.contains(HttpMethod.POST) && allow.contains(HttpMethod.TRACE) && allow.contains(HttpMethod.OPTIONS));
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(0, os.size());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void makeTraceRequest() throws IOException, InterruptedException {
		try {
			authenticator = null;
			sslEnabled = false;
			startServer();
			final URL url = new URL("http://localhost:" + port + HANDLER_PATH_PARAMS);
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			connection.setRequestMethod(HttpMethod.TRACE.toUpperCase());
			Assert.assertEquals(200, connection.getResponseCode());
			// Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			InputStream is = null;
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertNotEquals(0, os.size());
			log.info(os.toString());
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@Test
	public void testBacktrackingProtection() throws InterruptedException, IOException {
		try {
			InputStream tis = null;
			OutputStream tos = null;
			File allowed = null;
			try {
				File subDir = new File(certificate.getParentFile().getPath() + "/backtracking");
				subDir.mkdir();
				tis = getClass().getResourceAsStream("lorem-sm.txt");
				allowed = new File(subDir + "/allowed.txt");
				tos = new FileOutputStream(allowed);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			File denied = null;
			try {
				tis = getClass().getResourceAsStream("lorem-sm.txt");
				denied = File.createTempFile("denied", ".txt");
				tos = new FileOutputStream(denied);
				IOUtils.copy(tis, tos, 1024);
			}
			finally {
				IOUtils.closeQuietly(tos, tis);
			}

			authenticator = null;
			sslEnabled = false;
			startServer();
			URL url = new URL("http://localhost:" + port + "/backtracking/allowed.txt");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(200, connection.getResponseCode());
			Assert.assertEquals("200 " + HttpStatusCodes.getMap().get(200), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			Assert.assertEquals(loremSmallMd5, connection.getHeaderField("ETAG"));
			Assert.assertEquals(loremSmallTxt.length(), connection.getContentLength());
			Assert.assertTrue(connection.getContentType().startsWith("text/plain"));
			InputStream is = null;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				IOUtils.copy(is, os, loremSmallTxt.length() / 3);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();
			Assert.assertEquals(loremSmallTxt, os.toString());

			url = new URL("http://localhost:" + port + "/backtracking/..%2F/" + denied.getName());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(404, connection.getResponseCode());
			Assert.assertEquals("404 " + HttpStatusCodes.getMap().get(404), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			is = null;
			os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				Assert.assertFalse(true);
			}
			catch (final FileNotFoundException e) {
				Assert.assertNotNull(e);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();

			url = new URL("http://localhost:" + port + "/backtracking/../" + denied.getName());
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(20000);
			connection.setReadTimeout(20000);
			Assert.assertEquals(404, connection.getResponseCode());
			Assert.assertEquals("404 " + HttpStatusCodes.getMap().get(404), connection.getHeaderField("Status"));
			Assert.assertNotEquals(0, connection.getDate());
			is = null;
			os = new ByteArrayOutputStream();
			try {
				is = connection.getInputStream();
				Assert.assertFalse(true);
			}
			catch (final FileNotFoundException e) {
				Assert.assertNotNull(e);
			}
			finally {
				IOUtils.closeQuietly(os, is);
			}
			connection.disconnect();

			if (!allowed.delete()) {
				allowed.deleteOnExit();
			}
			if (!denied.delete()) {
				denied.deleteOnExit();
			}
		}
		catch (final SocketTimeoutException e) {
			log.log(Level.WARNING, "Undesirable exception", e);
			exceptions.add(e);
			Assume.assumeTrue(false);
		}
	}

	@After
	public void stopServer() throws InterruptedException {
		server.stop();
	}

}
