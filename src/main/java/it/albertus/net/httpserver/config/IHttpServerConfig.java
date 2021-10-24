package it.albertus.net.httpserver.config;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;

import it.albertus.net.httpserver.HttpPathHandler;

@SuppressWarnings("restriction")
public interface IHttpServerConfig {

	HttpPathHandler[] getHandlers();

	@Nullable
	Filter[] getFilters();

	@Nullable
	Authenticator getAuthenticator();

	boolean isEnabled();

	int getPort();

	boolean isCompressionEnabled();

	int getResponseBufferLimit();

	/** @return the maximum time a request is allowed to take, in seconds. */
	long getMaxReqTime();

	/** @return the maximum time a response is allowed to take, in seconds. */
	long getMaxRspTime();

	boolean isSslEnabled();

	char[] getStorePass();

	String getKeyStoreType();

	String getKeyStoreFileName();

	char[] getKeyPass();

	String getSslProtocol();

	String getKeyManagerFactoryAlgorithm();

	String getTrustManagerFactoryAlgorithm();

	SSLParameters getSslParameters(SSLContext context);

	/** @return the maximum number of threads to allow in the pool. */
	int getMaxThreadCount();

	/** @return the number of threads to keep in the pool, even if they are idle. */
	int getMinThreadCount();

	/**
	 * @return the maximum time that excess idle threads will wait for new tasks
	 *         before terminating, in seconds.
	 */
	long getThreadKeepAliveTime();

	/**
	 * @return the maximum time in seconds to wait until exchanges have finished.
	 */
	int getStopDelay();

	String getRequestLoggingLevel();

	String getResponseLoggingLevel();

	boolean isTraceMethodEnabled();

}
