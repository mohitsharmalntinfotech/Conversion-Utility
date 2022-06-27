package com.springboot;

import java.util.Map;

public class ResultModel {
	
	private String projectName;
	private boolean success;
	private String error;
	private String stackTrace;
	private Map<String, Boolean> responseMap;

	public ResultModel(String projectName, boolean success, String error, String stackTrace) {
		super();
		this.projectName = projectName;
		this.success = success;
		this.error = error;
		this.stackTrace = stackTrace;
	}

	public ResultModel() {}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	public Map<String, Boolean> getResponseMap() {
		return responseMap;
	}

	public void setResponseMap(Map<String, Boolean> responseMap) {
		this.responseMap = responseMap;
	}

	
	
}
