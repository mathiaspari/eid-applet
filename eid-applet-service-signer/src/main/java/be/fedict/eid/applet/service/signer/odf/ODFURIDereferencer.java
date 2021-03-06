/*
 * eID Applet Project.
 * Copyright (C) 2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */
package be.fedict.eid.applet.service.signer.odf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.xml.crypto.Data;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * JSR105 URI dereferencer implementation using an ODF file as resource
 * repository.
 * 
 * @author fcorneli
 * 
 */
public class ODFURIDereferencer implements URIDereferencer {

	private static final Log LOG = LogFactory.getLog(ODFURIDereferencer.class);
	private final URL odfUrl;
	private final byte[] odfData;
	private final URIDereferencer baseUriDereferener;
	private final DocumentBuilder documentBuilder;

	public ODFURIDereferencer(URL odfUrl) {
		this(odfUrl, null);
	}

	public ODFURIDereferencer(byte[] odfData) {
		this(null, odfData);
	}

	private ODFURIDereferencer(URL odfUrl, byte[] odfData) {
		if (null == odfUrl && null == odfData) {
			throw new IllegalArgumentException("odfUrl and odfData are null");
		}
		if (null != odfUrl && null != odfData) {
			throw new IllegalArgumentException(
					"odfUrl and odfData are both not null");
		}
		this.odfUrl = odfUrl;
		this.odfData = odfData;
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		this.baseUriDereferener = xmlSignatureFactory.getURIDereferencer();

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);

		try {
			this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(
					"parser config error: " + e.getMessage(), e);
		}
		EntityResolver entityResolver = new ODFEntityResolver();
		this.documentBuilder.setEntityResolver(entityResolver);
	}

	public Data dereference(URIReference uriReference, XMLCryptoContext context)
			throws URIReferenceException {
		if (null == uriReference) {
			throw new NullPointerException("URIReference cannot be null");
		}
		if (null == context) {
			throw new NullPointerException("XMLCrytoContext cannot be null");
		}

		String uri = uriReference.getURI();
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOG.warn("could not URL decode the uri: " + uri);
		}
		LOG.debug("dereference: " + uri);
		try {
			InputStream dataInputStream = findDataInputStream(uri);
			if (null == dataInputStream) {
				LOG
						.debug("cannot resolve, delegating to base DOM URI dereferener: "
								+ uri);
				return this.baseUriDereferener.dereference(uriReference,
						context);
			}
			if (uri.endsWith(".xml")) {
				/*
				 * We parse the XML ourselves as we might need to resolve MathML
				 * DTD.
				 */
				byte[] data = IOUtils.toByteArray(dataInputStream);
				if (0 == data.length) {
					return new OctetStreamData(dataInputStream, uri, null);
				}
				Document document = documentBuilder
						.parse(new ByteArrayInputStream(data));
				XMLSignatureInput xmlSignatureInput = new XMLSignatureInput(
						document);
				ApacheNodeSetData apacheNodeSetData = new ApacheNodeSetData(
						xmlSignatureInput);
				return apacheNodeSetData;
			}
			return new OctetStreamData(dataInputStream, uri, null);
		} catch (IOException e) {
			throw new URIReferenceException("I/O error: " + e.getMessage(), e);
		} catch (SAXException e) {
			throw new URIReferenceException("SAX error: " + e.getMessage(), e);
		}
	}

	/**
	 * Find a given file / zip entry in the ODF package
	 * 
	 * @param uri
	 * @return inputstream of the file
	 * @throws IOException
	 */
	private InputStream findDataInputStream(String uri) throws IOException {
		InputStream odfInputStream;
		if (null != this.odfUrl) {
			odfInputStream = this.odfUrl.openStream();
		} else {
			odfInputStream = new ByteArrayInputStream(this.odfData);
		}
		return ODFUtil.findDataInputStream(odfInputStream, uri);
	}
}
