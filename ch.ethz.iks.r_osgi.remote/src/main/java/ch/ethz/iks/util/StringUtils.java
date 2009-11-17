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
package ch.ethz.iks.util;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Version;

/**
 * String utilities.
 * 
 * @author Jan S. Rellermeyer, ETH Zurich
 * @since 0.2
 */
public final class StringUtils {

	/**
	 * hide the default constructor.
	 */
	private StringUtils() {
	}

	public static String[] splitString(final String values,
			final String delimiter) throws IllegalArgumentException {
		if (values == null) {
			return new String[0];
		}

		final List tokens = new ArrayList(values.length() / 10);
		int pointer = 0;
		int quotePointer = 0;
		int tokenStart = 0;
		int nextDelimiter;
		while ((nextDelimiter = values.indexOf(delimiter, pointer)) > -1) {
			int openingQuote = values.indexOf("\"", quotePointer);
			int closingQuote = values.indexOf("\"", openingQuote + 1);
			if (openingQuote > closingQuote) {
				throw new IllegalArgumentException(
						"Missing closing quotation mark.");
			}
			if (openingQuote > -1 && openingQuote < nextDelimiter
					&& closingQuote < nextDelimiter) {
				quotePointer = ++closingQuote;
				continue;
			}
			if (openingQuote < nextDelimiter && nextDelimiter < closingQuote) {
				pointer = ++closingQuote;
				continue;
			}
			// TODO: for performance, fold the trim into the splitting
			tokens.add(values.substring(tokenStart, nextDelimiter).trim());
			pointer = ++nextDelimiter;
			quotePointer = pointer;
			tokenStart = pointer;
		}
		tokens.add(values.substring(tokenStart).trim());
		return (String[]) tokens.toArray(new String[tokens.size()]);
	}

	/**
	 * splits a parameter (directives or attributes) into key and value
	 * 
	 * @param token
	 * @return
	 */
	public static String[] splitParameter(final String token)
			throws IllegalArgumentException {
		int pos = token.indexOf(":=");
		int offset = 2;
		if (pos < 0) {
			pos = token.indexOf("=");
			if (pos < 0) {
				throw new IllegalArgumentException("Malformed parameter "
						+ token);
			}
			offset = 1;
		}
		return new String[] { token.substring(0, pos),
				unQuote(token.substring(pos + offset, token.length())) };
	}

	public static String unQuote(final String quoted) {
		final int len = quoted.length();
		final int start = quoted.charAt(0) == '"' ? 1 : 0;
		final int end = quoted.charAt(quoted.length() - 1) == '"' ? len - 1
				: len;
		return (start == 0 && end == len) ? quoted : quoted.substring(start,
				end);
	}

	/**
	 * check, if the version is in the range of the version range specified by
	 * str
	 * 
	 * @param version
	 *            the Version to compare against the range
	 * @param str
	 *            String, that describes the version range
	 * @return true, if version in range
	 */
	public static boolean isVersionInRange(Version version, String str) {
		// System.out.println("    VERSION CHECK: "+version.toString()+" in range "+str+"?");

		// parse range
		if (str == null || str.length() < 1) {
			return (version.compareTo(Version.emptyVersion) > -1);
		}

		// remove "
		if (str.startsWith("\"")) {
			str = str.substring(1, str.length());
		}
		if (str.endsWith("\"")) {
			str = str.substring(0, str.length() - 1);
		}

		final String[] bounds = splitString(str, ",");
		if (bounds.length <= 1) {
			// range is only an "atleast value"
			Version v2 = new Version(str);
			if (version.compareTo(v2) < 0) {
				return false;
			}
		} else {
			// range has lower and upper bound
			final Version lower = new Version(bounds[0].substring(1).trim());
			final Version upper = new Version(bounds[1].substring(0,
					bounds[1].length() - 1).trim());
			// check lower bound
			if (bounds[0].startsWith("[")) {
				if (version.compareTo(lower) < 0) {
					return false;
				}
			} else {
				// assume "("
				if (version.compareTo(lower) <= 0) {
					return false;
				}
			}
			// check upper bound
			if (bounds[1].endsWith("]")) {
				if (version.compareTo(upper) > 0) {
					return false;
				}
			} else {
				// assume ")"
				if (version.compareTo(upper) >= 0) {
					return false;
				}
			}

		}
		return true;
	}
}
