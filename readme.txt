======= About This Project =======
This project is a Java library for connecting to Amazon S3 from Google App Engine.

This project will possibly be updated, but will not be supported or maintained.  It's purpose on here is to "share the love" just as @ogrodnek did, where we were able to fork from.

More information available at: http://socialappdev.com/using-amazon-s3-with-google-app-engine-02-2011

======= Other Java Amazon S3 Libraries =======
The code is a mix of these libraries:
s3-simple (forked) -> https://github.com/ogrodnek/s3-simple
s3-shell -> http://developer.amazonwebservices.com/connect/entry.jspa?externalID=138&categoryID=47
Java Base64 -> http://iharder.sourceforge.net/current/java/base64/
jets3t -> https://bitbucket.org/jmurty/jets3t/

Commentary on other libraries:
jets3t and Amazon's SDK are great but they use the Apache Commons http client which doesn't work 100% on Google App Engine.  This is due to restrictions... namely, no threads, no sockets, and... other restrictions...

Amazon SDK for Java was also tried, but was unable to run on Google's App Engine due to the restrictions mentioned above. -> http://aws.amazon.com/sdkforjava/

======= Changes =======
2011.03.08
- Added in the Ability to Create and Sign Policy Documents

2011.02.15
- Initial Changes

======= Features =======
/** Create an S3Store Object and Set the Current Bucket **/
S3Store s3 = new S3Store(Constants.S3_DEFAULT_HOSTNAME, ACCESS_KEY, SECRET_KEY);
s3.setBucket("my-bucket");

/** Sign an S3 URL **/
String signedUrl = s3.createSignedGETUrl(objectKey, validForInSeconds, isHttps);

/** Upload Binary Data to S3 (i.e. From the AppEngine Blobstore, etc) - App Engine Limits Uploads to 1MB **/
final Map<String, List<String>> headers = new HashMap<String, List<String>>();
headers.put("Content-Type", contentType);
s3.storeItem(key, bytes, Constants.ACL_PUBLIC_READ, headers);