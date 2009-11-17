/* Copyright (c) 2006-2009 Jan S. Rellermeyer
 * Systems Group,
 * Department of Computer Science, ETH Zurich.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of ETH Zurich nor the names of its contributors may be
 *      used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ch.ethz.iks.r_osgi.streams;

import java.io.IOException;
import java.io.InputStream;

import ch.ethz.iks.r_osgi.impl.ChannelEndpointImpl;

/**
 * Proxy object for input streams.
 * 
 * @author Michael Duller, ETH Zurich
 */
public class InputStreamProxy extends InputStream {

	/**
	 * the stream id.
	 */
	private final short streamID;

	/**
	 * the channel endpoint.
	 */
	private final ChannelEndpointImpl endpoint;

	/**
	 * Create a new input stream proxy.
	 * 
	 * @param streamID
	 *            the stream id.
	 * @param endpoint
	 *            the channel endpoint.
	 */
	public InputStreamProxy(final short streamID,
			final ChannelEndpointImpl endpoint) {
		this.streamID = streamID;
		this.endpoint = endpoint;
	}

	/**
	 * Read from the stream.
	 * 
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		return endpoint.readStream(streamID);
	}

	/**
	 * Read from the stream.
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(final byte[] b, final int off, final int len)
			throws IOException {
		return endpoint.readStream(streamID, b, off, len);
	}

}
