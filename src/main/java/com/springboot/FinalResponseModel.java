package com.springboot;

import java.util.List;
import java.util.Map;

public class FinalResponseModel {
	
	List<ResultModel> resultModelList;
	
	String errorMessage;
	
	String stackTrace;
	
	Map<String, Boolean> finalResponseMap;
	

	public Map<String, Boolean> getFinalResponseMap() {
		return finalResponseMap;
	}
	public void setFinalResponseMap(Map<String, Boolean> finalResponseMap) {
		this.finalResponseMap = finalResponseMap;
	}
	public FinalResponseModel(List<ResultModel> resultModelList, String errorMessage, String stackTrace) {
		super();
		this.resultModelList = resultModelList;
		this.errorMessage = errorMessage;
		this.stackTrace = stackTrace;
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
	public String getStackTrace() {
		return stackTrace;
	}
	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}
	
	
}
