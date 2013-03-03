
package com.myyearbook.hudson.plugins.confluence;

import java.net.URI;

/**
 * Utility methods
 *
 * @author Joe Hansche <jhansche@myyearbook.com>
 */
public class Util
{
    /** Relative path to resolve the XmlRpc endpoint URL */
    private static final String XML_RPC_URL_PATH = "rpc/xmlrpc";

    /** Relative path to resolve the SOAP endpoint URL */
    private static final String SOAP_URL_PATH = "rpc/soap-axis/confluenceservice-v1";

    /** Relative path to resolve the SOAP v2 endpoint URL */
    private static final String SOAP_V2_URL_PATH = "rpc/soap-axis/confluenceservice-v2";

    /**
     * Convert a generic Confluence URL into the XmlRpc endpoint URL
     *
     * @param url
     * @return
     * @see #XML_RPC_URL_PATH
     */
    public static String confluenceUrlToXmlRpcUrl(String url)
    {
        return URI.create(url).resolve(XML_RPC_URL_PATH).normalize().toString();
    }

    /**
     * Convert a generic Confluence URL into the SOAP endpoint URL
     *
     * @param url
     * @return
     * @see #SOAP_URL_PATH
     */
    public static String confluenceUrlToSoapUrl(String url)
    {
        return URI.create(url).resolve(SOAP_URL_PATH).normalize().toString();
    }

    public static String confluenceUrlToSoapV2Url(String url)
    {
        return URI.create(url).resolve(SOAP_V2_URL_PATH).normalize().toString();
    }
}
