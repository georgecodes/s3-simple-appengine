package com.handstandtech.amazon.s3;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmazonS3PolicyEncoder {

	private static Logger log = LoggerFactory.getLogger(AmazonS3PolicyEncoder.class
			.getName());

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
		log.trace("Policy Document: " + policyDocumentJSON);

		try {
			String policy = Base64ForAppEngine
					.encodeBytes(policyDocumentJSON.getBytes("UTF-8"))
					.replaceAll("\n", "").replaceAll("\r", "");

			Double encodePolicySeconds = (new Date().getTime() - one.getTime()) * .001;

			Date two = new Date();

			Mac hmac = Mac.getInstance("HmacSHA1");
			hmac.init(new SecretKeySpec(awsSecretKey.getBytes("UTF-8"),
					"HmacSHA1"));
			String signature = Base64ForAppEngine.encodeBytes(
					hmac.doFinal(policy.getBytes("UTF-8")))
					.replaceAll("\n", "");

			Double calculateSignatureSeconds = (new Date().getTime() - two
					.getTime()) * .001;

			log.info("Encode Policy [" + encodePolicySeconds
					+ " Seconds], Calculate Signature ["
					+ calculateSignatureSeconds + " Seconds].");
			
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