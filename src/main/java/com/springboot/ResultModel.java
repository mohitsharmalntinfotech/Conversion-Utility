package com.springboot;

public class ResultModel {
	
	private String projectName;
	private boolean success;
	private String error;
	
	

	public ResultModel(String projectName, boolean success, String error) {
		super();
		this.projectName = projectName;
		this.success = success;
		this.error = error;
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
	
	

	
	
}
