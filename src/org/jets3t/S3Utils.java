package org.jets3t;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.amazon.s3shell.Base64;

public class S3Utils {
	private static Logger log = Logger.getLogger(S3Utils.class.getName());

	public static List<String> getResourceParameterNames() {
		// Special HTTP parameter names that refer to resources in S3
		return Arrays
				.asList(new String[] { "acl", "policy", "torrent", "logging",
						"location", "requestPayment", "versions", "versioning",
						"versionId", "uploads", "uploadId", "partNumber" });
	}

	public static final List<String> HTTP_HEADER_METADATA_NAMES = Arrays
			.asList(new String[] { "content-type", "content-md5",
					"content-length", "content-language", "expires",
					"cache-control", "content-disposition", "content-encoding" });

	public static Map renameMetadataKeys(Map metadata) {
		Map convertedMetadata = new HashMap();
		// Add all meta-data headers.
		if (metadata != null) {
			Iterator metaDataIter = metadata.entrySet().iterator();
			while (metaDataIter.hasNext()) {
				Map.Entry entry = (Map.Entry) metaDataIter.next();
				String key = (String) entry.getKey();
				Object value = entry.getValue();

				if (!HTTP_HEADER_METADATA_NAMES.contains(key.toLowerCase(Locale
						.getDefault()))
						&& !key.startsWith(Constants.REST_HEADER_PREFIX)) {
					key = Constants.REST_METADATA_PREFIX + key;
				}
				convertedMetadata.put(key, value);
			}
		}
		return convertedMetadata;
	}

	/**
	 * Encodes a URL string, and ensures that spaces are encoded as "%20"
	 * instead of "+" to keep fussy web browsers happier.
	 * 
	 * @param path
	 * @return encoded URL.
	 * @throws ServiceException
	 */
	public static String encodeUrlString(String path) throws ServiceException {
		try {
			String encodedPath = URLEncoder.encode(path,
					Constants.DEFAULT_ENCODING);
			// Web browsers do not always handle '+' characters well, use the
			// well-supported '%20' instead.
			encodedPath = encodedPath.replaceAll("\\+", "%20");
			// '@' character need not be URL encoded and Google Chrome balks on
			// signed URLs if it is.
			encodedPath = encodedPath.replaceAll("%40", "@");
			return encodedPath;
		} catch (UnsupportedEncodingException uee) {
			throw new ServiceException("Unable to encode path: " + path, uee);
		}
	}

	/**
	 * Calculate the canonical string for a REST/HTTP request to a storage
	 * service.
	 * 
	 * When expires is non-null, it will be used instead of the Date header.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public static String makeServiceCanonicalString(String method,
			String resource, Map<String, Object> headersMap, String expires,
			String headerPrefix, List<String> serviceResourceParameterNames)
			throws UnsupportedEncodingException {
		StringBuffer canonicalStringBuf = new StringBuffer();
		canonicalStringBuf.append(method + "\n");

		// Add all interesting headers to a list, then sort them. "Interesting"
		// is defined as Content-MD5, Content-Type, Date, and x-amz-
		SortedMap<String, Object> interestingHeaders = new TreeMap<String, Object>();
		if (headersMap != null && headersMap.size() > 0) {
			for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
				Object key = entry.getKey();
				Object value = entry.getValue();

				if (key == null) {
					continue;
				}
				String lk = key.toString().toLowerCase(Locale.getDefault());

				// Ignore any headers that are not particularly interesting.
				if (lk.equals("content-type") || lk.equals("content-md5")
						|| lk.equals("date") || lk.startsWith(headerPrefix)) {
					interestingHeaders.put(lk, value);
				}
			}
		}

		// Remove default date timestamp if "x-amz-date" is set.
		if (interestingHeaders
				.containsKey(Constants.REST_METADATA_ALTERNATE_DATE)) {
			interestingHeaders.put("date", "");
		}

		// Use the expires value as the timestamp if it is available. This
		// trumps both the default
		// "date" timestamp, and the "x-amz-date" header.
		if (expires != null) {
			interestingHeaders.put("date", expires);
		}

		// these headers require that we still put a new line in after them,
		// even if they don't exist.
		if (!interestingHeaders.containsKey("content-type")) {
			interestingHeaders.put("content-type", "");
		}
		if (!interestingHeaders.containsKey("content-md5")) {
			interestingHeaders.put("content-md5", "");
		}

		// Finally, add all the interesting headers (i.e.: all that start with
		// x-amz- ;-))
		for (Map.Entry<String, Object> entry : interestingHeaders.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (key.startsWith(headerPrefix)) {
				canonicalStringBuf.append(key).append(':').append(value);
			} else {
				canonicalStringBuf.append(value);
			}
			canonicalStringBuf.append("\n");
		}

		// don't include the query parameters...
		int queryIndex = resource.indexOf('?');
		if (queryIndex == -1) {
			canonicalStringBuf.append(resource);
		} else {
			canonicalStringBuf.append(resource.substring(0, queryIndex));
		}

		// ...unless the parameter(s) are in the set of special params
		// that actually identify a service resource.
		if (queryIndex >= 0) {
			SortedMap<String, String> sortedResourceParams = new TreeMap<String, String>();

			// Parse parameters from resource string
			String query = resource.substring(queryIndex + 1);
			for (String paramPair : query.split("&")) {
				String[] paramNameValue = paramPair.split("=");
				String name = URLDecoder.decode(paramNameValue[0], "UTF-8");
				String value = null;
				if (paramNameValue.length > 1) {
					value = URLDecoder.decode(paramNameValue[1], "UTF-8");
				}
				// Only include parameter (and its value if present) in
				// canonical
				// string if it is a resource-identifying parameter
				if (serviceResourceParameterNames.contains(name)) {
					sortedResourceParams.put(name, value);
				}
			}

			// Add resource parameters
			if (sortedResourceParams.size() > 0) {
				canonicalStringBuf.append("?");
			}
			boolean addedParam = false;
			for (Map.Entry<String, String> entry : sortedResourceParams
					.entrySet()) {
				if (addedParam) {
					canonicalStringBuf.append("&");
				}
				canonicalStringBuf.append(entry.getKey());
				if (entry.getValue() != null) {
					canonicalStringBuf.append("=" + entry.getValue());
				}
				addedParam = true;
			}
		}

		return canonicalStringBuf.toString();
	}

	/**
	 * Calculate the HMAC/SHA1 on a string.
	 * 
	 * @param awsSecretKey
	 *            AWS secret key.
	 * @param canonicalString
	 *            canonical string representing the request to sign.
	 * @return Signature
	 * @throws S3ServiceException
	 */
	public static String signWithHmacSha1(String awsSecretKey,
			String canonicalString) throws ServiceException {
		if (awsSecretKey == null) {
			log.log(Level.INFO,
					"Canonical string will not be signed, as no AWS Secret Key was provided");
			return null;
		}

		// The following HMAC/SHA1 code for the signature is taken from the
		// AWS Platform's implementation of RFC2104
		// (amazon.webservices.common.Signature)
		//
		// Acquire an HMAC/SHA1 from the raw key bytes.
		SecretKeySpec signingKey = null;
		try {
			signingKey = new SecretKeySpec(
					awsSecretKey.getBytes(Constants.DEFAULT_ENCODING),
					Constants.HMAC_SHA1_ALGORITHM);
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException(
					"Unable to get bytes from secret string", e);
		}

		// Acquire the MAC instance and initialize with the signing key.
		Mac mac = null;
		try {
			mac = Mac.getInstance(Constants.HMAC_SHA1_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// should not happen
			throw new RuntimeException("Could not find sha1 algorithm", e);
		}
		try {
			mac.init(signingKey);
		} catch (InvalidKeyException e) {
			// also should not happen
			throw new RuntimeException(
					"Could not initialize the MAC algorithm", e);
		}

		// Compute the HMAC on the digest, and set it.
		try {
			return Base64.encodeBytes(mac.doFinal(canonicalString
					.getBytes(Constants.DEFAULT_ENCODING)));
			// return new String(b64);
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException(
					"Unable to get bytes from canonical string", e);
		}
	}

	public static String generateS3HostnameForBucket(String bucketName,
			boolean isDnsBucketNamingDisabled, String s3Endpoint) {
		if (isBucketNameValidDNSName(bucketName) && !isDnsBucketNamingDisabled) {
			return bucketName + "." + s3Endpoint;
		} else {
			return s3Endpoint;
		}
	}

	/**
	 * Returns true if the given bucket name can be used as a component of a
	 * valid DNS name. If so, the bucket can be accessed using requests with the
	 * bucket name as part of an S3 sub-domain. If not, the old-style bucket
	 * reference URLs must be used, in which case the bucket name must be the
	 * first component of the resource path.
	 * 
	 * @param bucketName
	 *            the name of the bucket to test for DNS compatibility.
	 */
	public static boolean isBucketNameValidDNSName(String bucketName) {
		if (bucketName == null || bucketName.length() > 63
				|| bucketName.length() < 3) {
			return false;
		}

		// Only lower-case letters, numbers, '.' or '-' characters allowed
		if (!Pattern.matches("^[a-z0-9][a-z0-9.-]+$", bucketName)) {
			return false;
		}

		// Cannot be an IP address, i.e. must not contain four '.'-delimited
		// sections with 1 to 3 digits each.
		if (Pattern.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}", bucketName)) {
			return false;
		}

		// Components of name between '.' characters cannot start or end with
		// '-',
		// and cannot be empty
		String[] fragments = bucketName.split("\\.");
		for (int i = 0; i < fragments.length; i++) {
			if (Pattern.matches("^-.*", fragments[i])
					|| Pattern.matches(".*-$", fragments[i])
					|| Pattern.matches("^$", fragments[i])) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Encodes a URL string but leaves a delimiter string unencoded. Spaces are
	 * encoded as "%20" instead of "+".
	 * 
	 * @param path
	 * @param delimiter
	 * @return encoded URL string.
	 * @throws ServiceException
	 */
	public static String encodeUrlPath(String path, String delimiter)
			throws ServiceException {
		StringBuffer result = new StringBuffer();
		String tokens[] = path.split(delimiter);
		for (int i = 0; i < tokens.length; i++) {
			result.append(encodeUrlString(tokens[i]));
			if (i < tokens.length - 1) {
				result.append(delimiter);
			}
		}
		return result.toString();
	}
}
