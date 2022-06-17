package com.springboot;

public class SourceDestinationModel {

	String source;
	String destination;
	
	SourceDestinationModel(){}
	
	public SourceDestinationModel(String source, String destination) {
		this.source = source;
		this.destination = destination;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	

}
