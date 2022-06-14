package com.springboot;

import java.util.List;

public class FinalResponseModel {
	
	List<ResultModel> resultModelList;
	
	String errorMessage;
	
	public FinalResponseModel(List<ResultModel> resultModelList, String errorMessage) {
		super();
		this.resultModelList = resultModelList;
		this.errorMessage = errorMessage;
	}
	public FinalResponseModel() {}
	
	public List<ResultModel> getResultModelList() {
		return resultModelList;
	}
	public void setResultModelList(List<ResultModel> resultModelList) {
		this.resultModelList = resultModelList;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	
}
