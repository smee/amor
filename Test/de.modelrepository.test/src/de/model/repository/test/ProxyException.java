package de.model.repository.test;

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.emf.common.util.URI;

public class ProxyException extends Exception {
	private static final long serialVersionUID = -7347816416175683110L;
	private Vector<URI> proxyURIs;
	
	/**
	 * Constructs a new proxy exception with the specified detail message. The cause is not initialized, and may subsequently be initialized by a call to {@link Throwable#initCause(java.lang.Throwable)}. 
	 * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()} method.
	 * @param proxyURIs a {@link Vector} containing the {@link URI}s of the proxies which couldn't be resolved.
	 */
	public ProxyException(String message, Vector<URI> proxyURIs) {
		super(message);
		this.proxyURIs = proxyURIs;
	}
	
	/**
	 * Constructs a new proxy exception. The cause is not initialized, and may subsequently be initialized by a call to {@link Throwable#initCause(java.lang.Throwable)}.
	 * @param proxyURIs a {@link Vector} containing the {@link URI}s of the proxies which couldn't be resolved.
	 */
	public ProxyException(Vector<URI> proxyURIs) {
		this.proxyURIs = proxyURIs;
	}
	
	/**
	 * Constructs a new proxy exception with the specified detail message and cause. 
	 * @param message the detail message. The detail message is saved for later retrieval by the {@link Throwable#getMessage()} method.
	 * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 * @param proxyURIs a {@link Vector} containing the {@link URI}s of the proxies which couldn't be resolved.
	 */
	public ProxyException(String message, Throwable cause, Vector<URI> proxyURIs) {
		super(message, cause);
		this.proxyURIs = proxyURIs;
	}
	
	/**
	 * Constructs a new proxy exception with the specified cause. 
	 * @param cause the cause (which is saved for later retrieval by the {@link Throwable#getCause()} method). (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 * @param proxyURIs a {@link Vector} containing the {@link URI}s of the proxies which couldn't be resolved.
	 */
	public ProxyException(Throwable cause, Vector<URI> proxyURIs) {
		super(cause);
		this.proxyURIs = proxyURIs;
	}
	
	/**
	 * @return The proxies which are missing because of missing libraries.
	 */
	public Iterator<URI> getMissingProxies() {
		return proxyURIs.iterator();
	}
}
