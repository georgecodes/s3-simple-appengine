package com.handstandtech.amazon.s3;


/**
 * Holds the Policy and Signature
 * 
 * @author Sam Edwards
 */
public class PolicyAndSignature {
	private String policy;
	private String signature;

	public PolicyAndSignature() {

	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public String getPolicy() {
		return policy;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public String toString() {
		return "Policy: " + policy + "\nSignature: " + signature + "\n";
	}

	
}