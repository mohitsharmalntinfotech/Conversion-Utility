package com.springboot;

import static com.springboot.constant.UtilConstant.APP_PROP;
import static com.springboot.constant.UtilConstant.IMPORT_RESOURCE_STATMNT;
import static com.springboot.constant.UtilConstant.OPENAPI_CMD;
import static com.springboot.constant.UtilConstant.OPENAPI_CONFIG_CLASS;
import static com.springboot.constant.UtilConstant.OPENAPI_HEALTH_CLASS;
import static com.springboot.constant.UtilConstant.OPENAPI_SPRINGBOOT_CLASS;
import static com.springboot.constant.UtilConstant.PACKAGE_NAME;
import static com.springboot.constant.UtilConstant.POM_XML;
import static com.springboot.constant.UtilConstant.SPRINGBOOT_ANNOTATION_NAME;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SpringBootApplication
@RestController
public class ProjectCreaterApplication {

	@Value("${destn.javafile}")
	private String destnJavaFile;

	@Value("${springint.fileName}")
	private String destnSpringIntFileName;

	private String fileSeparator = FileSystems.getDefault().getSeparator();
	
	public static void main(String[] args) throws IOException {
		SpringApplication.run(ProjectCreaterApplication.class, args);
	}

	@EventListener
	public void onApplicationReadyEvent(ApplicationReadyEvent event) throws IOException {
		System.out.println("Start the App to build SI integration");
	}

	@GetMapping("/health")
	public String healthCheck() {
		
		return "APP is UP!!";
	}
	
	@PostMapping("/multiProjectMigrateWithGIT")
	public FinalResponseModel utilCallForGit(@RequestBody SourceDestinationModel sourceDestModel,
			@RequestHeader(value = "sourceToken", required = false) String sourceToken,
			@RequestHeader(value = "destToken", required = false) String destToken) {
		FinalResponseModel finalResponseModel = new FinalResponseModel();
		
		Map<String, String> finalResponseHashMap = getDefaultFinalResponseMap();	
		List<ResultModel> resultModelList = new ArrayList<>();
		String sourceRepo = sourceDestModel.getSource();
		String destinationRepo = sourceDestModel.getDestination();

		String defaultBaseDir = System.getProperty("java.io.tmpdir");
		long currentTimeMillis = System.currentTimeMillis();
		
		String sourceMultiDir = defaultBaseDir + fileSeparator +"Source" + currentTimeMillis;
		String destinationMultiDir = defaultBaseDir + fileSeparator + "DesDir" + currentTimeMillis;
		String gitFolder = defaultBaseDir + fileSeparator + "Destination" + currentTimeMillis; 
		
		File[] directories = null;
		String sourceDir = "";
		String destinationDir = "";

		try {
			cloneSourceGitRepo(sourceRepo, sourceMultiDir, sourceToken);
			finalResponseHashMap.put("Clone from Git","Completed"); //Source cloned from Git
			
			directories = new File(sourceMultiDir).listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					if (file.getName().equalsIgnoreCase(".git")) {
						return false;
					} else
						return file.isDirectory();
				}
			});
			finalResponseHashMap.put("Directories Identified","Completed"); //Directories Identified
			finalResponseModel.setFinalResponseMap(finalResponseHashMap);
			
		}catch (Exception ex) {
			finalResponseModel.setResultModelList(resultModelList);
			finalResponseModel.setErrorMessage(ex.getLocalizedMessage());
			finalResponseModel.setStackTrace(convertStackTraceToString(ex));
			finalResponseModel.setFinalResponseMap(finalResponseHashMap);
			
		}
		if(directories!=null) {
			
			for (File localSourceDirectory : directories) {
				ResultModel model = new ResultModel();
				Map projectResponseMap = getDefaultResponseMap();
				try {
					sourceDir = sourceMultiDir + fileSeparator + localSourceDirectory.getName();
					destinationDir = destinationMultiDir + fileSeparator + localSourceDirectory.getName();
					String cmd = OPENAPI_CMD + destinationDir;
					Process p = Runtime.getRuntime().exec(cmd);
					Thread.sleep(8000);
					projectResponseMap.put("1","Completed"); //Open API command executed
					directoryCleanUp(destinationDir);
					projectResponseMap.put("2","Completed"); //Directory cleaned up
					String mainBootFileName = modifyMainJavaFile(sourceDir, destinationDir);
					projectResponseMap.put("3","Completed"); //Main Java file modified
					modifyPropFile(sourceDir, destinationDir);
					projectResponseMap.put("4","Completed"); //Properties file modified
					modifyPOMFile(sourceDir, destinationDir);
					projectResponseMap.put("5","Completed"); //POM file modified
					createIntegrationXMLFile(sourceDir, destinationDir, mainBootFileName);
					projectResponseMap.put("6","Completed"); //Integration XML created
					model.setProjectName(localSourceDirectory.getName());
					model.setSuccess(true);
					model.setError("no Error");
					model.setResponseMap(projectResponseMap);
				}catch (Exception ex) {
					model.setProjectName(localSourceDirectory.getName());
					model.setSuccess(false);
					model.setError(ex.getMessage());
					model.setStackTrace(convertStackTraceToString(ex));
					model.setResponseMap(projectResponseMap);
					
				}
				resultModelList.add(model);
			}
			finalResponseModel.setResultModelList(resultModelList);
		}
		try {
			cloneDestinationGitRepoAndCommit(destinationRepo, destinationMultiDir, gitFolder, destToken);
			finalResponseHashMap.put("Push Projects to Git","Completed"); //Push to Destination
		}catch (Exception ex) {
			finalResponseModel.setResultModelList(resultModelList);
			finalResponseModel.setErrorMessage(ex.getLocalizedMessage());
			finalResponseModel.setStackTrace(convertStackTraceToString(ex));
		}

		//resultModelList.add(model);
		return finalResponseModel;
	}

	private Map<String, String> getDefaultFinalResponseMap() {
		Map<String, String> finalResponseHashMap = new HashMap<String, String>();
		finalResponseHashMap.put("Clone from Git","Not initiated yet.."); //Source cloned from Git
		finalResponseHashMap.put("Directories Identified","Not initiated yet.."); //Directories Identified
		finalResponseHashMap.put("Push Projects to Git","Not initiated yet.."); //Push to Destination
		return finalResponseHashMap;
	}

	private Map getDefaultResponseMap() {
		Map projectResponseMap = new HashMap<String, String>();
		projectResponseMap.put("1","Not Initiated.."); //Open API command executed
		projectResponseMap.put("2","Not Initiated.."); //Directory cleaned up
		projectResponseMap.put("3","Not Initiated.."); //Main Java file modified
		projectResponseMap.put("4","Not Initiated.."); //Properties file modified
		projectResponseMap.put("5","Not Initiated.."); //POM file modified
		projectResponseMap.put("6","Not Initiated.."); //Integration XML created
		return projectResponseMap;
	}

	private static String convertStackTraceToString(Throwable throwable) 
	  {
	      try (StringWriter sw = new StringWriter(); 
	             PrintWriter pw = new PrintWriter(sw)) 
	      {
	          throwable.printStackTrace(pw);
	          return sw.toString();
	      } 
	      catch (IOException ioe) 
	      {
	          throw new IllegalStateException(ioe);
	      }
	  }   
	
	private void createIntegrationXMLFile(String sourceDir, String destinationDir, String mainBootFileName) throws Exception {

		// Get Document Builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		String sourceMuleFileName = getMuleFileLocation(sourceDir);
		String sourceMuleXMLlocation = getDirectoryNameForFile(sourceDir, sourceMuleFileName);
		Document document = builder.parse(new File(sourceMuleXMLlocation));
		document.getDocumentElement().normalize();

		Map<String, String> xmlNamespaceMap = modifyXmlNameSpace(document);
		StringBuilder writeXml = getNamespaceValues(xmlNamespaceMap);
		getFlowAttribute(document, writeXml);
		
		writeXml.append(
				"\n<integration:channel id=\"outbound\"/>\r\n"
						+ " <integration:header-enricher input-channel=\"requestChannel\" output-channel=\"outbound\"/>\r\n"
						+ "   ");
		
		writeXml.append("\n</beans>");
		createIntegrationXMLFile(writeXml, destinationDir);
	}


	private StringBuilder getFlowAttribute(Document document, StringBuilder writeFlowSb) throws URISyntaxException, IOException {
				
		InputStream integrationMappingFileReader = Model.class.getClassLoader().getResourceAsStream("mule-integration-mapping.properties");

		Properties siMappingPropertiesFile = new Properties();
		siMappingPropertiesFile.load(integrationMappingFileReader);
		
		NodeList flowTagList = document.getElementsByTagName("flow");
		
		NodeList flowNode = flowTagList.item(0).getChildNodes();
		for (int i = 0; i < flowNode.getLength(); i++) {
			Node childNode = (Node) flowNode.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				if(childNode.getNodeName().equalsIgnoreCase("http:listener")) {
					writeFlowSb = writeFlowSb.append(httpListnerConversion(siMappingPropertiesFile.getProperty(childNode.getNodeName().replace(":", "-")), childNode));
				}else if(childNode.getNodeName().equalsIgnoreCase("jms:publish")) {
					writeFlowSb = writeFlowSb.append(jmsPublishConversion(siMappingPropertiesFile.getProperty(childNode.getNodeName().replace(":", "-")), childNode));
				}else if(childNode.getNodeName().equalsIgnoreCase("ee:enricher")) {
					writeFlowSb = writeFlowSb.append(enricherConversion(siMappingPropertiesFile.getProperty(childNode.getNodeName().replace(":", "-")), childNode));
				}
			}

		}
		return writeFlowSb;
	}

	private Object enricherConversion(String property, Node childNode) {

		NodeList nChildList = childNode.getChildNodes();
		Node enricherVariableNode = null;
		Node node = nChildList.item(1);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			NodeList nodeSubChild = node.getChildNodes();
			for (int i = 0; i < nodeSubChild.getLength(); i++) {
				Node subNode = nodeSubChild.item(i);
				if (subNode.getNodeName().equalsIgnoreCase("set-variable")) {
					enricherVariableNode = subNode;
					break;
				}
			}
		}

		StringBuilder writeEnricherSb = new StringBuilder();
		String[] splitEnricherProp = property.split(",");
		writeEnricherSb = writeEnricherSb.append("\n<");
		for (int i = 0; i < splitEnricherProp.length; i++) {
			if (splitEnricherProp[i].contains("=?")) {
				if (splitEnricherProp[i].contains("expression")) {
					String attributeValue = getChildTagAttributes(enricherVariableNode,	splitEnricherProp[i].replace("expression=?", "value"));
					attributeValue = attributeValue.replace("#[", "").replace("]", "").replace("+", ",");
					String[] splitAttributeValue = attributeValue.split(",");
					attributeValue = attributeValue.replace(splitAttributeValue[0], "payload").replace(",", "+");
					splitEnricherProp[i] = splitEnricherProp[i].replace("?", "\""+attributeValue+"\"");
				}
			}
			writeEnricherSb = writeEnricherSb.append(splitEnricherProp[i] + " ");
		}
		writeEnricherSb.append("/>");
		System.out.println(writeEnricherSb);
		return writeEnricherSb;
	}

	private StringBuilder jmsPublishConversion(String property, Node childNode) {
		StringBuilder writeJmsPublishSb = new StringBuilder();
		String[] splitJmsPublishProp = property.split(",");
		writeJmsPublishSb = writeJmsPublishSb.append("\n<");
		for(int i=0; i<splitJmsPublishProp.length; i++) {
			
			if(splitJmsPublishProp[i].contains("=?")) {
				String propKey = null;
				if(splitJmsPublishProp[i].contains("destination-name")) {
					propKey = splitJmsPublishProp[i].replace("-name=?", "");
				}else {
					propKey = splitJmsPublishProp[i].replace("=?", "");
				}
				String attributeValue = getChildTagAttributes(childNode, propKey);
				splitJmsPublishProp[i] = splitJmsPublishProp[i].replace("?", "\""+attributeValue+"\"");
			}
			writeJmsPublishSb = writeJmsPublishSb.append(splitJmsPublishProp[i]+" ");
		}
		
		writeJmsPublishSb.append("/>");
		
		return writeJmsPublishSb;
	}

	private StringBuilder httpListnerConversion(String property, Node childNode) {
		StringBuilder writeHttpListnerSb = new StringBuilder();
		String[] splitHttpListnerProp = property.split(",");
		writeHttpListnerSb = writeHttpListnerSb.append("\n <");
		for(int i=0; i<splitHttpListnerProp.length; i++) {
			
			if(splitHttpListnerProp[i].contains("=?")) {
				String attributeValue = getChildTagAttributes(childNode, splitHttpListnerProp[i].replace("=?", ""));
				splitHttpListnerProp[i] = splitHttpListnerProp[i].replace("?", "\""+attributeValue+"\"");
			}
			writeHttpListnerSb = writeHttpListnerSb.append(splitHttpListnerProp[i]+" ");
		}
		
		writeHttpListnerSb.append("/>");
		
		writeHttpListnerSb.append("\n"
				+ "	<integration:channel id=\"requestChannel\"/>\r\n"
				+ "    <integration:channel id=\"outputChannel\"/>");
		
		
		return writeHttpListnerSb;
		
	}
	
	private String getChildTagAttributes(Node childNode, String data) {
		NamedNodeMap attributesMap = childNode.getAttributes();
		String dataValue = null;
		for (int a = 0; a < attributesMap.getLength(); a++) {
			Node nodeTheAttribute = attributesMap.item(a);
			if (nodeTheAttribute.getNodeName().equalsIgnoreCase(data)){
				dataValue = nodeTheAttribute.getNodeValue();
			}
		}
		
		return dataValue;
	}

	private StringBuilder getNamespaceValues(Map<String, String> xmlNamespaceMap) {

		StringBuilder xmlStringBuilder = new StringBuilder();
		xmlStringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n  <beans  ");
		for (Map.Entry<String, String> entry : xmlNamespaceMap.entrySet()) {
			xmlStringBuilder.append("\n" + entry.getKey() + "=" + "\"" + entry.getValue() + "\"");
		}
		xmlStringBuilder.append(">");

		return xmlStringBuilder;
	}

	private Map<String, String> modifyXmlNameSpace(Document document)
			throws FileNotFoundException, URISyntaxException, IOException {
		NodeList muleTagList = document.getElementsByTagName("mule");
		Map<String, String> xmlNamespaceMap = new HashMap<>();
		xmlNamespaceMap.put("xmlns:integration", "http://www.springframework.org/schema/integration");


		InputStream in = Model.class.getClassLoader().getResourceAsStream("namespace-mapping.properties");		
		
		Properties nameSpacePropertiesFile = new Properties();
		nameSpacePropertiesFile.load(new InputStreamReader(in));
		
		InputStream schemaStream = Model.class.getClassLoader().getResourceAsStream("schema-mapping.properties");		

		Properties schemaPropertiesFile = new Properties();
		schemaPropertiesFile.load(new InputStreamReader(schemaStream));

		Node node = muleTagList.item(0);

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Node theAttribute = null;
			String muleXmlNameSpace = null;
			String springNameSpace = null;
			NamedNodeMap attributes = node.getAttributes();
			StringBuilder sb;
			for (int a = 0; a < attributes.getLength(); a++) {
				theAttribute = attributes.item(a);
				if (theAttribute.getNodeName().equals("xsi:schemaLocation")) {

					String[] splitSchemaLocation = theAttribute.getNodeValue().split(" ");
					int length = splitSchemaLocation.length;
					sb = new StringBuilder();
					sb = sb.append(
							"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd ");
					for (int i = 0; i < length; i++) {
						String schemaName = splitSchemaLocation[i].replace(":", "-");
						String springSchemaName = schemaPropertiesFile.getProperty(schemaName);
						if (springSchemaName != null) {
							sb = sb.append(springSchemaName);
							if (i < length - 1) {
								sb.append(" ");
							}

						}

					}

					xmlNamespaceMap.put(theAttribute.getNodeName(), sb.toString());

				} else {
					muleXmlNameSpace = theAttribute.getNodeName().replace(":", "-");
					springNameSpace = nameSpacePropertiesFile.getProperty(muleXmlNameSpace);
					if (springNameSpace != null) {
						String[] splitNameSpace = springNameSpace.split(",");
						xmlNamespaceMap.put(splitNameSpace[0], splitNameSpace[1]);
					}
				}

			}

			System.out.println(xmlNamespaceMap);

		}

		return xmlNamespaceMap;
	}


	private void directoryCleanUp(String destinationDir) throws IOException {
		String destnConfigDeleteLocation = getDirectoryNameForFile(destinationDir, OPENAPI_CONFIG_CLASS);
		destnConfigDeleteLocation = destnConfigDeleteLocation.replace(fileSeparator + OPENAPI_CONFIG_CLASS, "");
		String destnAPIDeleteLocation = getDirectoryNameForFile(destinationDir, OPENAPI_HEALTH_CLASS);
		destnAPIDeleteLocation = destnAPIDeleteLocation.replace(fileSeparator + OPENAPI_HEALTH_CLASS, "");
		//String destnFormatterLocation = getDirectoryNameForFile(destinationDir, FORMATTER_CLASS);
		File fileDirectoryConfig = new File(destnConfigDeleteLocation);
		File fileDirectoryAPI = new File(destnAPIDeleteLocation);
		//File fileDirectoryFormatter = new File(destnFormatterLocation);
		FileUtils.deleteDirectory(fileDirectoryConfig);
		FileUtils.deleteDirectory(fileDirectoryAPI);
		//FileUtils.forceDelete(fileDirectoryFormatter);

	}

	public String modifyMainJavaFile(String sourceDir, String destinationDir)
			throws IOException, XmlPullParserException {

		String mainBootFileName = "SpringBootApp";
		StringBuffer sb = new StringBuffer();
		String destnJavaFileLocation = getDirectoryNameForFile(destinationDir, destnJavaFile);
		FileReader fr = new FileReader(destnJavaFileLocation);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains(" " + OPENAPI_SPRINGBOOT_CLASS) || line.contains(OPENAPI_SPRINGBOOT_CLASS + ".class")) {
				line = line.replace(OPENAPI_SPRINGBOOT_CLASS, mainBootFileName);
				sb.append(line);
				sb.append('\n');
			} else {
				sb.append(line);
				sb.append('\n');
				if (line.contains(PACKAGE_NAME)) {
					// add import statement for importResources
					sb.append(IMPORT_RESOURCE_STATMNT);
					sb.append('\n');
				}
				if (line.contains(SPRINGBOOT_ANNOTATION_NAME)) {
					// add importResource annotation with XML name
					StringBuffer newSb = new StringBuffer();
					newSb.append("@ImportResource(\"classpath*:/").append(destnSpringIntFileName).append("\")");
					sb.append(newSb);
					sb.append('\n');
				}
			}

		}
		br.close();
		writingLogic(destnJavaFileLocation, sb, false);
		renamingFile(mainBootFileName, destnJavaFileLocation);
		return mainBootFileName;

	}

	private void renamingFile(String mainBootFileName, String destnJavaFileLocation) {
		File file = new File(destnJavaFileLocation);
		String newdestnJavaFileLocation = destnJavaFileLocation.replace(OPENAPI_SPRINGBOOT_CLASS + ".java", "")
				+ mainBootFileName + ".java";
		File newJavafile = new File(newdestnJavaFileLocation);
		file.renameTo(newJavafile);
	}

	public void modifyPropFile(String sourceDir, String destinationDir) throws Exception {

		String nodeVal = null;
		String sourceMuleFileName = getMuleFileLocation(sourceDir);
		String sourceMuleXMLlocation = getDirectoryNameForFile(sourceDir, sourceMuleFileName);
		File fXmlFile = new File(sourceMuleXMLlocation);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();

		ConcurrentHashMap<String, ConcurrentHashMap<String, String>> outerMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

		InputStream in = Model.class.getClassLoader().getResourceAsStream("config.properties");			
		
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = br.readLine()) != null) {
			ConcurrentHashMap<String, String> innerMap = new ConcurrentHashMap<String, String>();
			String[] arr = line.split("=");
			String nodeName = arr[0];
			String attrList = arr[1];
			String[] attrArray = attrList.split(",");
			for (String attrName : attrArray) {
				innerMap.put(attrName, "");
			}
			outerMap.put(nodeName, innerMap);
		}

		for (String nodeName : outerMap.keySet()) {
			ConcurrentHashMap<String, String> localInnerMap = outerMap.get(nodeName);
			for (String nodeAttr : localInnerMap.keySet()) {
				nodeVal = documentParserValue(doc, nodeName, nodeAttr);
				localInnerMap.put(nodeAttr, nodeVal);
			}
			outerMap.put(nodeName, localInnerMap);
		}

		StringBuffer sb = new StringBuffer();
		for (String nodeName : outerMap.keySet()) {
			ConcurrentHashMap<String, String> localInnerMap = outerMap.get(nodeName);
			for (String nodeAttr : localInnerMap.keySet()) {

				if (nodeName.equalsIgnoreCase("jms:factory-configuration") && nodeAttr.equalsIgnoreCase("brokerUrl")) {
					sb.append("spring.activemq.broker-url=").append(localInnerMap.get(nodeAttr)).append('\n');
				}
				if (nodeName.equalsIgnoreCase("jms:active-mq-connection") && nodeAttr.equalsIgnoreCase("username")) {
					sb.append("spring.activemq.user=").append(localInnerMap.get(nodeAttr)).append('\n');
				}
				if (nodeName.equalsIgnoreCase("jms:active-mq-connection") && nodeAttr.equalsIgnoreCase("password")) {
					sb.append("spring.activemq.password=").append(localInnerMap.get(nodeAttr)).append('\n');
				}
			}

		}

		String destnAppPropLocation = getDirectoryNameForFile(destinationDir, APP_PROP);
		writingLogic(destnAppPropLocation, sb, true);

	}

	private String getMuleFileLocation(String sourceDir) {
		String file = "";
		String sourceMuleFolderlocation = getDirectoryNameForFile(sourceDir, "mule");
		File listFiles[] = new File(sourceMuleFolderlocation).listFiles();
		if (listFiles.length > 1) {
			Arrays.sort(listFiles, (f1, f2) -> {
				return new Long(f2.length()).compareTo(new Long(f1.length()));
			});
		}
		file = listFiles[0].getName();

		return file;
	}

	public void modifyPOMFile(String sourceDir, String destinationDir) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		String sourceMulePOMlocation = getDirectoryNameForFile(sourceDir, POM_XML);
		Document document = builder.parse(new File(sourceMulePOMlocation));
		document.getDocumentElement().normalize();

		Element root = document.getDocumentElement();

		NodeList nList = document.getElementsByTagName("dependency");
		ArrayList<String> siDependencyList = new ArrayList<>();

		InputStream in = Model.class.getClassLoader().getResourceAsStream("muledependencies.properties");	

		Properties propertiesFile = new Properties();
		propertiesFile.load(new InputStreamReader(in));

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node node = nList.item(temp);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) node;
				Node muleNode = elem.getElementsByTagName("artifactId").item(0);
				String dependencyName = propertiesFile.getProperty(muleNode.getTextContent());
				if (dependencyName != null) {
					siDependencyList.add(dependencyName);
				}
			}
		}
		writeDependenciestoDestnPOM(siDependencyList, destinationDir);
		// modifyArtifact(sourceDir, destinationDir);
	}

	private void modifyArtifact(String sourceDir, String destinationDir)
			throws IOException, XmlPullParserException, FileNotFoundException {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		String sourceMulePOMlocation = getDirectoryNameForFile(sourceDir, POM_XML);
		Model modelSource = reader.read(new FileReader(sourceMulePOMlocation));

		String destMulePOMlocation = getDirectoryNameForFile(destinationDir, POM_XML);
		MavenXpp3Reader readerNew = new MavenXpp3Reader();
		Model modelDest = readerNew.read(new FileReader(destMulePOMlocation));
		modelDest.setArtifactId(modelSource.getArtifactId());

		MavenXpp3Writer writer = new MavenXpp3Writer();
		writer.write(new FileWriter(destMulePOMlocation), modelDest);
	}

	private void createIntegrationXMLFile(StringBuilder xmlStringBuilder, String destinationDir) throws Exception {
		String destnResourceLocation = getDirectoryNameForFile(destinationDir, "resources");

		String fileName = destnResourceLocation + fileSeparator + destnSpringIntFileName;
		File file = new File(fileName);
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(file));
		writer.append(xmlStringBuilder);

		writer.close();
	}

	public void writeDependenciestoDestnPOM(ArrayList<String> siDependencyList, String destinationDir)
			throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String destnPOMLocation = getDirectoryNameForFile(destinationDir, POM_XML);
		File myFile = new File(destnPOMLocation);
		Document document = builder.parse(myFile);
		document.getDocumentElement().normalize();
		Element root = document.getDocumentElement();

		NodeList nList = document.getElementsByTagName("dependencies");

		int temp = nList.getLength() - 1;
		Node node = nList.item(temp);

		for (String depencyName : siDependencyList) {
			node.appendChild(root.appendChild(createDependency(document, depencyName)));
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();
		transf.setOutputProperty(OutputKeys.INDENT, "yes");

		DOMSource source = new DOMSource(document);

		StreamResult console = new StreamResult(System.out);
		StreamResult file = new StreamResult(myFile);

		transf.transform(source, console);
		transf.transform(source, file);

	}

	private Node createDependency(Document doc, String dependencyName) {
		Element user = doc.createElement("dependency");
		String[] splitName = dependencyName.split(",");
		user.appendChild(createDependencyElement(doc, "groupId", splitName[0]));
		user.appendChild(createDependencyElement(doc, "artifactId", splitName[1]));
		return user;
	}

	private Node createDependencyElement(Document doc, String name, String value) {
		Element node = doc.createElement(name);
		node.appendChild(doc.createTextNode(value));
		return node;
	}

	public static String getDirectoryNameForFile(String dir, String fileName) {
		File[] files = new File(dir).listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				String loc = getDirectoryNameForFile(f.getPath(), fileName);
				if (loc != null)
					return loc;
			}
			if (f.getName().equals(fileName))
				return f.getPath();
		}
		return null;
	}

	private void writingLogic(String fileLocation, StringBuffer sb, boolean EOF) throws IOException {
		FileWriter writer = new FileWriter(fileLocation, EOF);
		writer.write(sb.toString());
		writer.close();
	}

	private String documentParserValue(Document doc, String elementName, String elementNodeName) {
		NodeList nList = doc.getElementsByTagName(elementName);
		String value = null;
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				value = eElement.getAttribute(elementNodeName);
			}
		}
		return value;
	}

	private void createDynamicIntegrationFile(String sourceDir, String destinationDir) throws Exception {

		// Get Document Builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		String sourceMuleFileName = getMuleFileLocation(sourceDir);
		String sourceMulePOMlocation = getDirectoryNameForFile(sourceDir, sourceMuleFileName);
		Document document = builder.parse(new File(sourceMulePOMlocation));
		document.getDocumentElement().normalize();

		NodeList muleTagList = document.getElementsByTagName("mule");
		Map<String, String> xmlNamespaceMap = new HashMap<>();
		xmlNamespaceMap.put("xmlns:integration", "\"http://www.springframework.org/schema/integration\"");

		FileReader reader = new FileReader(
				new File(getClass().getClassLoader().getResource("muletosimapping.properties").toURI()));

		Properties propertiesFile = new Properties();
		propertiesFile.load(reader);

		Node node = muleTagList.item(0);

		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Node theAttribute = null;
			String muleXmlNameSpace = null;
			String springNameSpace = null;
			NamedNodeMap attributes = node.getAttributes();
			for (int a = 0; a < attributes.getLength(); a++) {
				theAttribute = attributes.item(a);
				muleXmlNameSpace = theAttribute.getNodeName().replace(":", "-");
				springNameSpace = propertiesFile.getProperty(muleXmlNameSpace);
				if (springNameSpace != null) {
					String[] splitNameSpace = springNameSpace.split(",");
					xmlNamespaceMap.put(splitNameSpace[0], splitNameSpace[1]);
				}

			}

			System.out.println(xmlNamespaceMap);

		}

		NodeList flowTagList = document.getElementsByTagName("flow");

		NodeList flowNode = flowTagList.item(0).getChildNodes();
		// NodeList nodes= flowNode.getChildNodes();
		System.out.println(flowNode.getLength());
		for (int i = 0; i < flowNode.getLength(); i++) {
			Node childNode = (Node) flowNode.item(i);
			System.out.println("Node Name : " + childNode.getNodeName());
			System.out.println("Node getNodeType : " + childNode.getNodeType());
			System.out.println("Node getNextSibling : " + childNode.getNextSibling());
			System.out.println("Node getTextContent : " + childNode.getTextContent());
			System.out.println("Node getNodeValue : " + childNode.getNodeValue());
			System.out.println("Node getNamespaceURI : " + childNode.getNamespaceURI());
		}

	}

	private static void cloneSourceGitRepo(String sourceRepoUrl, String clonedSourceDirectory, String token)
			throws Exception {
		File localPath = new File(clonedSourceDirectory);
		Git.cloneRepository().setURI(sourceRepoUrl).setCredentialsProvider(configAuthentication(token, ""))
				.setDirectory(localPath).call();
	}

	private static UsernamePasswordCredentialsProvider configAuthentication(String user, String password) {
		return new UsernamePasswordCredentialsProvider(user, password);
	}

	private static void cloneDestinationGitRepoAndCommit(String destinationRepoUrl, String destinationFolder,
			String gitFolder, String token) throws Exception {
		Git git = Git.cloneRepository().setURI(destinationRepoUrl)
				.setCredentialsProvider(configAuthentication(token, "")).setDirectory(Paths.get(gitFolder).toFile())
				.call();
		copyFiles(destinationFolder, gitFolder); // copy generated projects from local destination folder to GIT folder
													// for commit
		git.add().addFilepattern(".").call();
		String commitMessage = "commit message " + Math.random();
		git.commit().setMessage(commitMessage).call();
		git.push().setCredentialsProvider(configAuthentication(token, "")).call();
	}

	private static void copyFiles(String source, String dest) throws Exception {
		Path destinationFolder = Paths.get(source);
		Path destinationGITFolder = Paths.get(dest);

		Files.walk(destinationFolder).forEach(sourcePath -> {
			try {
				Path targetPath = destinationGITFolder.resolve(destinationFolder.relativize(sourcePath));
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

	}

}
