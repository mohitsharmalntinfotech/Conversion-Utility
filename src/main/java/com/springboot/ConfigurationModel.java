package com.springboot;

public class ConfigurationModel {

	
	private String basePackage;
	private String configPackage;
	private String apiPackage;
	private String modelPackage;
	private String groupId;
	private String artifactId;
	
	public ConfigurationModel() {}
	
	public ConfigurationModel(String basePackage, String configPackage, String apiPackage, String modelPackage,
			String groupId, String artifactId) {
		super();
		this.basePackage = basePackage;
		this.configPackage = configPackage;
		this.apiPackage = apiPackage;
		this.modelPackage = modelPackage;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}
	public String getBasePackage() {
		return basePackage;
	}
	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}
	public String getConfigPackage() {
		return configPackage;
	}
	public void setConfigPackage(String configPackage) {
		this.configPackage = configPackage;
	}
	public String getApiPackage() {
		return apiPackage;
	}
	public void setApiPackage(String apiPackage) {
		this.apiPackage = apiPackage;
	}
	public String getModelPackage() {
		return modelPackage;
	}
	public void setModelPackage(String modelPackage) {
		this.modelPackage = modelPackage;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	
	
	
}