package com.handstandtech.amazon.s3;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the information needed to upload to amazon S3
 * 
 * @author Sam Edwards
 */
public class S3Info {
	private String AWSAccessKeyId;
	private String redirect;
	private Date expiration;
	private String bucket;
	private String uploadPath;
	private String acl;
	private String contentType;
	private Integer minContentLength;
	private Integer maxContentLength;
	private PolicyAndSignature policyAndSignature;

	/**
	 * Default Constructor
	 */
	public S3Info() {

	}

	public Integer getMinContentLength() {
		return minContentLength;
	}

	public void setMinContentLength(Integer minContentLength) {
		this.minContentLength = minContentLength;
	}

	public Integer getMaxContentLength() {
		return maxContentLength;
	}

	public void setMaxContentLength(Integer maxContentLength) {
		this.maxContentLength = maxContentLength;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getAWSAccessKeyId() {
		return AWSAccessKeyId;
	}

	public void setAWSAccessKeyId(String aWSAccessKeyId) {
		AWSAccessKeyId = aWSAccessKeyId;
	}

	public String getRedirect() {
		return redirect;
	}

	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getBucket() {
		return bucket;
	}

	public void setUploadPath(String uploadPath) {
		this.uploadPath = uploadPath;
	}

	public String getUploadPath() {
		return uploadPath;
	}

	public String getPolicyDocumentJSON() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		JSONObject json = new JSONObject();
		try {
			json.put("expiration", sdf.format(this.expiration));
			JSONArray conditions = new JSONArray();
			conditions.put(new JSONObject().put("bucket", this.bucket));
			conditions.put(new JSONArray().put(0, "starts-with").put(1, "$key")
					.put(2, this.uploadPath));
			// acl
			conditions.put(new JSONObject().put("acl", this.acl));
			// redirect
			conditions.put(new JSONArray().put(0, "starts-with")
					.put(1, "$success_action_redirect").put(2, this.redirect));
			// Content-Type
			conditions.put(new JSONArray().put(0, "starts-with")
					.put(1, "$Content-Type").put(2, this.contentType));
			// content-length-range
			conditions.put(new JSONArray().put(0, "content-length-range")
					.put(1, minContentLength).put(2, maxContentLength));

			json.put("conditions", conditions);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json.toString();
	}

	public void setAcl(String acl) {
		this.acl = acl;
	}

	public String getAcl() {
		return acl;
	}

	public void setPolicyAndSignature(PolicyAndSignature policyAndSignature) {
		this.policyAndSignature = policyAndSignature;
	}

	public PolicyAndSignature getPolicyAndSignature() {
		return policyAndSignature;
	}
}