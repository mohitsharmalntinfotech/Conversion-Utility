package com.springboot;

public class ResultModel {
	
	private String projectName;
	private boolean success;
	
	public ResultModel(String projectName, boolean success) {
		super();
		this.projectName = projectName;
		this.success = success;
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

	
	
}
