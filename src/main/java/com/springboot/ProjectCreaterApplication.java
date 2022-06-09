package com.springboot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootApplication
@RestController
public class ProjectCreaterApplication {

//	@Value("${source.mule.directory}")
//	private String sourceProjectLocation;

//	@Value("${destn.integration.directory}")
//	private String destnProjectLocation;

	@Value("${source.mulexml}")
	private String sourceMuleXML;

	@Value("${destn.javafile}")
	private String destnJavaFile;

	@Value("${springint.fileName}")
	private String destnSpringIntFileName;

	private static final String PACKAGE_NAME = "package";
	private static final String SPRINGBOOT_ANNOTATION_NAME = "@SpringBootApplication";
	private static final String IMPORT_RESOURCE_STATMNT = "import org.springframework.context.annotation.ImportResource;";


	public static void main(String[] args) throws IOException {
		SpringApplication.run(ProjectCreaterApplication.class, args);
	}

	@EventListener
	public void onApplicationReadyEvent(ApplicationReadyEvent event) throws IOException {
		System.out.println("Hello");
	}

	@PostMapping("/migrate")
	String utilCall(@RequestBody SourceDestinationModel sourceDestModel) throws Exception {
		String sourceDir = sourceDestModel.getSource();
		String destinationDir = sourceDestModel.getDestination();
		
		String cmd = "java -jar openapi-generator-cli-4.3.1.jar generate -g spring -i openapi.yaml -c conf.json -o "
				+ destinationDir;
		Process p = Runtime.getRuntime().exec(cmd);
		Thread.sleep(8000);
			
		deleteDirectories(destinationDir);
		String mainBootFileName = modifyJavaClass(destinationDir);
		modifyPropFile(sourceDir, destinationDir);
		modifyPOM(sourceDir, destinationDir);
		createIntegrationFile(sourceDir, destinationDir,mainBootFileName);
		
		return "Application migrated to spring boot successfully";
	}

	
	private void createIntegrationFile(String sourceDir, String destinationDir, String mainBootFileName) throws Exception {
		try {

			String sourceMuleXMLlocation = getDirectoryNameForFile(sourceDir, sourceMuleXML);

			XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(sourceMuleXMLlocation));
			StringBuilder xmlStringBuilder = new StringBuilder();
			xmlStringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
					+ "<beans xmlns=\"http://www.springframework.org/schema/beans\"\r\n"
					+ "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
					+ "       xmlns:jms=\"http://www.springframework.org/schema/integration/jms\"\r\n"
					+ "       xmlns:integration=\"http://www.springframework.org/schema/integration\"\r\n"
					+ "       xmlns:int-http=\"http://www.springframework.org/schema/integration/http\"\r\n"
					+ "       xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/integration/jms http://www.springframework.org/schema/integration/jms/spring-integration-jms.xsd http://www.springframework.org/schema/integration/http\r\n"
					+ "    http://www.springframework.org/schema/integration/http/spring-integration-http.xsd\">");
			while (reader.hasNext()) {
				XMLEvent nextEvent = reader.nextEvent();
				if (nextEvent.isStartElement()) {
					StartElement startElement = nextEvent.asStartElement();
					String startTagName = startElement.getName().getLocalPart();

					switch (startTagName) {

					case "listener":
						xmlStringBuilder.append("\r\n");
						xmlStringBuilder.append("<int-http:inbound-gateway\r\n"
								+ "		request-channel=\"requestChannel\" reply-channel=\"outputChannel\"");
						Iterator<Attribute> attributeList = startElement.getAttributes();
						while (attributeList.hasNext()) {
							Attribute attribute = attributeList.next();
							String attributeName = attribute.getName().getLocalPart();
							String attributeValue = attribute.getValue();
							if (attributeName.equals("path")) {
								xmlStringBuilder.append(" path=\"" + attributeValue + "\">");
							}

						}

						xmlStringBuilder.append("</int-http:inbound-gateway>\r\n"
								+ "	<integration:channel id=\"requestChannel\"/>\r\n"
								+ "    <integration:channel id=\"outputChannel\"/>");

						break;

					case "publish":
						xmlStringBuilder.append("\r\n");
						xmlStringBuilder.append(
								"<jms:message-driven-channel-adapter id=\"helloJMSAdapater\" destination-name=\"hello.queue\"\r\n"
										+ "        channel=\"inbound\"/>\r\n" + "\r\n"
										+ "    <integration:channel id=\"inbound\"/>\r\n"
										+ "    <integration:channel id=\"outbound\"/>\r\n"
										+ "    <integration:service-activator input-channel=\"requestChannel\" output-channel=\"outbound\" ref=\"simpleMessageListener\" method=\"onMessage\" />\r\n"
										+ "    <jms:outbound-channel-adapter id=\"jmsOut\" channel=\"outbound\"");

						Iterator<Attribute> attributeList2 = startElement.getAttributes();
						while (attributeList2.hasNext()) {
							Attribute attribute = attributeList2.next();
							String attributeName = attribute.getName().getLocalPart();
							String attributeValue = attribute.getValue();
							if (attributeName.equals("destination")) {
								xmlStringBuilder.append(" destination-name=\"" + attributeValue + "\"");
							}

						}

						xmlStringBuilder.append("/>");

						break;

					}

				}

				if (nextEvent.isEndElement()) {
					EndElement endElement = nextEvent.asEndElement();
					String endTagName = endElement.getName().getLocalPart();
					if (endTagName.equals("mule")) {
						xmlStringBuilder.append("</beans>");
					}
				}
			}

			createIntegrationXMLFile(xmlStringBuilder, destinationDir);
			
		
			createListnerFile(destinationDir,mainBootFileName);

		}

		catch (XMLStreamException | IOException e) {
			e.printStackTrace();
		}

	}

	private void createListnerFile(String destinationDir, String mainBootFileName) throws Exception, IOException {
		
		String destnListnerLocation = getDirectoryNameForFile(destinationDir, mainBootFileName);
		destnListnerLocation = destnListnerLocation.replace("\\"+mainBootFileName, "");
		File listnerFile = new File(destnListnerLocation+"\\"+"SimpleMessageListener.java");
		BufferedWriter listnerWriter = null;
		StringBuffer sb = new StringBuffer();
		sb.append("package com.lti;\n"
				+ "\n"
				+ "import org.springframework.stereotype.Component;\n"
				+ "\n"
				+ "@Component\n"
				+ "public class SimpleMessageListener {\n"
				+ "\n"
				+ "	public String onMessage() {\n"
				+ "		\n"
				+ "		return \"Published\";\n"
				+ "	}\n"
				+ "	\n"
				+ "}");
		try {
			
			listnerWriter = new BufferedWriter(new FileWriter(listnerFile));
			listnerWriter.append(sb);
			
		}

		catch (Exception e) {
			throw e;
		}

		finally {
			if (listnerWriter != null)
				listnerWriter.close();
		}
	}

	private void createIntegrationXMLFile(StringBuilder xmlStringBuilder, String  destinationDir) throws Exception, IOException {
		String destnResourceLocation = getDirectoryNameForFile(destinationDir, "resources");

		String fileName = destnResourceLocation + "//" + destnSpringIntFileName;
		File file = new File(fileName);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.append(xmlStringBuilder);
		}

		catch (Exception e) {
			throw e;
		}

		finally {
			if (writer != null)
				writer.close();
		}
	}

	public String modifyJavaClass(String destinationDir) throws IOException {
		String mainBootFileName = "myName.java";
		StringBuffer sb = new StringBuffer();
		String destnJavaFileLocation = getDirectoryNameForFile(destinationDir, destnJavaFile);
		FileReader fr = new FileReader(destnJavaFileLocation);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			if(line.contains(" OpenAPI2SpringBoot") || line.contains("OpenAPI2SpringBoot.class")) {
				line= line.replace("OpenAPI2SpringBoot", mainBootFileName);
				sb.append(line);
				sb.append('\n');
			}else {
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
		
		File file = new File(destnJavaFileLocation);
		String newdestnJavaFileLocation = destnJavaFileLocation.replace("OpenAPI2SpringBoot.java", "") + mainBootFileName;
		File newJavafile =new File(newdestnJavaFileLocation);
		file.renameTo(newJavafile);
		return mainBootFileName;
		
	}

	public void modifyPropFile(String sourceDir, String destinationDir) {

		try {
			String nodeVal = null;
			String sourceMuleXMLlocation = getDirectoryNameForFile(sourceDir, sourceMuleXML);
			File fXmlFile = new File(sourceMuleXMLlocation);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			ConcurrentHashMap<String, ConcurrentHashMap<String, String>> outerMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

			FileReader fr = new FileReader(
					new File(getClass().getClassLoader().getResource("config.properties").toURI()));
			BufferedReader br = new BufferedReader(fr);
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

					if (nodeName.equalsIgnoreCase("jms:factory-configuration")
							&& nodeAttr.equalsIgnoreCase("brokerUrl")) {
						sb.append("spring.activemq.broker-url=").append(localInnerMap.get(nodeAttr)).append('\n');
					}
					if (nodeName.equalsIgnoreCase("jms:active-mq-connection")
							&& nodeAttr.equalsIgnoreCase("username")) {
						sb.append("spring.activemq.user=").append(localInnerMap.get(nodeAttr)).append('\n');
					}
					if (nodeName.equalsIgnoreCase("jms:active-mq-connection")
							&& nodeAttr.equalsIgnoreCase("password")) {
						sb.append("spring.activemq.password=").append(localInnerMap.get(nodeAttr)).append('\n');
					}
				}

			}

			String destnAppPropLocation = getDirectoryNameForFile(destinationDir, "application.properties");
			writingLogic(destnAppPropLocation, sb, true);
			

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public void modifyPOM(String sourceDir, String destinationDir) throws URISyntaxException {
		// Get Document Builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			String sourceMulePOMlocation = getDirectoryNameForFile(sourceDir, "pom.xml");
			Document document = builder.parse(new File(sourceMulePOMlocation));
			document.getDocumentElement().normalize();

			Element root = document.getDocumentElement();

			NodeList nList = document.getElementsByTagName("dependency");
			ArrayList<String> siDependencyList = new ArrayList<>();

			FileReader reader = new FileReader(
					new File(getClass().getClassLoader().getResource("muledependencies.properties").toURI()));

			Properties propertiesFile = new Properties();
			propertiesFile.load(reader);

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

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeDependenciestoDestnPOM(ArrayList<String> siDependencyList, String destinationDir)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String destnPOMLocation = getDirectoryNameForFile(destinationDir, "pom.xml");
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

		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transf = transformerFactory.newTransformer();
			// transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transf.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(document);

			StreamResult console = new StreamResult(System.out);
			StreamResult file = new StreamResult(myFile);

			transf.transform(source, console);
			transf.transform(source, file);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
	
	private void deleteDirectories(String destinationDir) throws IOException {
		String destnDeleteLocation1 = getDirectoryNameForFile(destinationDir, "OpenAPIDocumentationConfig.java");
		destnDeleteLocation1 = destnDeleteLocation1.replace("\\OpenAPIDocumentationConfig.java", "");
		String destnDeleteLocation2 = getDirectoryNameForFile(destinationDir, "HealthApiController.java");
		destnDeleteLocation2 = destnDeleteLocation2.replace("\\HealthApiController.java", "");
		File f1 = new File(destnDeleteLocation1);
		File f2 = new File(destnDeleteLocation2);
		FileUtils.deleteDirectory(f1);
		FileUtils.deleteDirectory(f2);
		
	}

	
	private void createDynamicIntegrationFile(String sourceDir, String destinationDir) {

		// Get Document Builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			String sourceMulePOMlocation = getDirectoryNameForFile(sourceDir, sourceMuleXML);
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
//			NodeList nodes= flowNode.getChildNodes();
			System.out.println(flowNode.getLength());
			for(int i=0; i<flowNode.getLength(); i++) {
				Node childNode = (Node)flowNode.item(i); 
				System.out.println("Node Name : "+childNode.getNodeName());
				System.out.println("Node getNodeType : "+childNode.getNodeType());
				System.out.println("Node getNextSibling : "+childNode.getNextSibling());
				System.out.println("Node getTextContent : "+childNode.getTextContent());
				System.out.println("Node getNodeValue : "+childNode.getNodeValue());
				System.out.println("Node getNamespaceURI : "+childNode.getNamespaceURI());
			}
			

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (SAXException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	
	private void overrideJsonFile(ConfigurationModel conf, String destinationDir) throws URISyntaxException, IOException {

		//ConfigurationModel conf = sourceDestModel.getConf();
		
//		ObjectMapper objectMapper = new ObjectMapper();
//		String destnConfLocation = getDirectoryNameForFile(System.getProperty("user.dir"), "conf.json");
//		ConfigurationModel initialConfModel = objectMapper.readValue( new File(destnConfLocation), ConfigurationModel.class);
		
//		if(conf!=null) {
//			overrideJsonFile(conf,destinationDir);
//		}

		
		//objectMapper.writeValue(new File(destnConfLocation), initialConfModel);
		
		ObjectMapper objectMapper = new ObjectMapper();
		String destnConfLocation = getDirectoryNameForFile(System.getProperty("user.dir"), "conf.json");
		ConfigurationModel localConfModel = objectMapper.readValue( new File(destnConfLocation), ConfigurationModel.class);
		if(!StringUtils.isEmpty(conf.getArtifactId())) {
			localConfModel.setArtifactId(conf.getArtifactId());
		}
		if(!StringUtils.isEmpty(conf.getGroupId())) {
			localConfModel.setGroupId(conf.getGroupId());
		}
		if(!StringUtils.isEmpty(conf.getBasePackage())) {
			localConfModel.setBasePackage(conf.getBasePackage());
		}
		if(!StringUtils.isEmpty(conf.getApiPackage())) {
			localConfModel.setApiPackage(conf.getApiPackage());
		}
		if(!StringUtils.isEmpty(conf.getConfigPackage())) {
			localConfModel.setConfigPackage(conf.getConfigPackage());
		}
		if(!StringUtils.isEmpty(conf.getModelPackage())) {
			localConfModel.setModelPackage(conf.getModelPackage());
		}
		objectMapper.writeValue(new File(destnConfLocation), localConfModel);		
	}

}
