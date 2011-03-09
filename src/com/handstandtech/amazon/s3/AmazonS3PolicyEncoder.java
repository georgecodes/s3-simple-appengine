package com.handstandtech.amazon.s3;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AmazonS3PolicyEncoder {

	/**
	 * Calculates the Policy and Signature from <code>this</code>
	 */
	public static PolicyAndSignature calculatePolicyAndSignature(S3Info s3info,
			String awsSecretKey) {
		return getSignatureFromPolicyDocument(s3info, awsSecretKey);
	}

	public static PolicyAndSignature getSignatureFromPolicyDocument(
			S3Info s3Info, String awsSecretKey) {
		return getSignatureFromPolicyDocument(s3Info.getPolicyDocumentJSON(),
				awsSecretKey);
	}

	/**
	 * Create Signature from Policy Document
	 * 
	 * @param policyDocumentJSON
	 *            The Policy Document
	 * @return The {@link String} signature.
	 */
	public static PolicyAndSignature getSignatureFromPolicyDocument(
			String policyDocumentJSON, String awsSecretKey) {

		Date one = new Date();
		PolicyAndSignature policyAndSignature = new PolicyAndSignature();
		System.out.println(policyDocumentJSON);

		try {
			String policy = Base64ForAppEngine
					.encodeBytes(policyDocumentJSON.getBytes("UTF-8"))
					.replaceAll("\n", "").replaceAll("\r", "");

			System.out.println((new Date().getTime() - one.getTime()) * .001
					+ " Seconds to Encode Policy.");

			Date two = new Date();

			Mac hmac = Mac.getInstance("HmacSHA1");
			hmac.init(new SecretKeySpec(awsSecretKey.getBytes("UTF-8"),
					"HmacSHA1"));
			String signature = Base64ForAppEngine.encodeBytes(
					hmac.doFinal(policy.getBytes("UTF-8")))
					.replaceAll("\n", "");

			System.out.println((new Date().getTime() - two.getTime()) * .001
					+ " Seconds to Calculate Signature.");

			policyAndSignature.setPolicy(policy);
			policyAndSignature.setSignature(signature);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return policyAndSignature;
	}

}