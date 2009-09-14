/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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
 * 
 */
package org.openrdf.server.metadata.behaviours;

import static java.lang.Integer.toHexString;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.server.metadata.concepts.Transaction;
import org.openrdf.server.metadata.concepts.WebResource;

public abstract class WebResourceSupport implements WebResource {

	public String revisionTag() {
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		return "W/" + '"' + revision + '"';
	}

	public String identityTag() {
		Transaction trans = getRevision();
		String mediaType = getMediaType();
		if (trans == null || mediaType == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		String type = toHexString(mediaType.hashCode());
		String schema = toHexString(getObjectConnection().getSchemaRevision());
		return '"' + revision + '-' + type + '-' + schema + '"';
	}

	public String variantTag(String mediaType) {
		if (mediaType == null)
			return revisionTag();
		Transaction trans = getRevision();
		if (trans == null)
			return null;
		String uri = trans.getResource().stringValue();
		String revision = toHexString(uri.hashCode());
		String variant = toHexString(mediaType.hashCode());
		String schema = toHexString(getObjectConnection().getSchemaRevision());
		return "W/" + '"' + revision + '-' + variant + '-' + schema + '"';
	}

	public long lastModified() {
		Transaction trans = getRevision();
		if (trans == null)
			return 0;
		XMLGregorianCalendar xgc = trans.getCommittedOn();
		if (xgc == null)
			return 0;
		GregorianCalendar cal = xgc.toGregorianCalendar();
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	public void extractMetadata(File file) {
		// no metadata
	}
}
