Last Update to Readme: 2011/02/15

=======

About: 

This project is a simple java library for connecting to Amazon S3 from Google App Engine.

The code is a mix of these libraries:
s3-simple (forked) -> https://github.com/ogrodnek/s3-simple
s3-shell -> http://developer.amazonwebservices.com/connect/entry.jspa?externalID=138&categoryID=47
Java Base64 -> http://iharder.sourceforge.net/current/java/base64/
jets3t -> https://bitbucket.org/jmurty/jets3t/

jets3t and Amazon's SDK is great but it uses the Apache Commons http client which doesn't work 100% on app engine.  This is due to restrictions around google app engine... namely, no threads, no sockets, and... other restrictions...

Another library I tried was the Amazon SDK for Java.  It is also unable to run on Google's App Engine due to the restrictions mentioned above. -> http://aws.amazon.com/sdkforjava/

=======

Features:

//Create an S3Store Object and Set the Current Bucket
S3Store s3 = new S3Store("s3.amazonaws.com", ACCESS_KEY, SECRET_KEY);
s3.setBucket("my-bucket");

//Sign an S3 URL
String signedUrl = s3.createSignedGETUrl(objectKey, validForInSeconds, isHttps);

//Upload Binary Data to S3 (i.e. From the AppEngine Blobstore, etc)
final Map<String, List<String>> headers = new HashMap<String, List<String>>();
headers.put("Content-Type", contentType);
s3.storeItem(key, bytes, Constants.ACL_PUBLIC_READ, headers);