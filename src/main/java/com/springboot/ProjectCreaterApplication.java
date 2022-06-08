package com.springboot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@SpringBootApplication
@RestController
public class ProjectCreaterApplication {

	@Value("${source.mule.directory}")
	private String sourceProjectLocation;
	
	@Value("${destn.integration.directory}")
	private String destnProjectLocation;

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
		//Process p = Runtime.getRuntime().exec(
			//	"java -jar openapi-generator-cli-4.3.1.jar generate -g spring -i openapi.yaml -c conf.json -o D:\\spring-boot-codeg");
	}
	
	
	@GetMapping("/0")
	void utilCall() throws IOException, URISyntaxException, InterruptedException {
		String cmd = "java -jar openapi-generator-cli-4.3.1.jar generate -g spring -i openapi.yaml -c conf.json -o "+ destnProjectLocation;
		Process p = Runtime.getRuntime().exec(cmd);
		Thread.sleep(3000);
		modifyJavaClass();
		modifyPropFile();
		modifyPOM();
	}

	@GetMapping("/1")
	public void modifyJavaClass() throws IOException {
		StringBuffer sb = new StringBuffer();
		String destnJavaFileLocation = getDirectoryNameForFile(destnProjectLocation,destnJavaFile);
		FileReader fr = new FileReader(destnJavaFileLocation);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
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
		br.close();
		writingLogic(destnJavaFileLocation, sb,false);
	}

	@GetMapping("/2")
	public void modifyPropFile() {

		try {
			String nodeVal = null;
			String sourceMuleXMLlocation = getDirectoryNameForFile(sourceProjectLocation,sourceMuleXML);
			File fXmlFile = new File(sourceMuleXMLlocation);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			ConcurrentHashMap<String, ConcurrentHashMap<String, String>> outerMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

			// FileReader fr = new
			// FileReader("C:\\Users\\10705555\\eclipse-workspace\\New\\src\\main\\resources\\myProp.properties");

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

			String destnAppPropLocation = getDirectoryNameForFile(destnProjectLocation,"application.properties");
			writingLogic(destnAppPropLocation, sb,true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writingLogic(String fileLocation, StringBuffer sb, boolean EOF) throws IOException {
		FileWriter writer = new FileWriter(fileLocation,EOF);
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

	@GetMapping("/3")
	public void modifyPOM() throws URISyntaxException {
		// Get Document Builder
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			String sourceMulePOMlocation = getDirectoryNameForFile(sourceProjectLocation,"pom.xml");
			Document document = builder.parse(new File(sourceMulePOMlocation));
			document.getDocumentElement().normalize();

			Element root = document.getDocumentElement();

			NodeList nList = document.getElementsByTagName("dependency");
			ArrayList<String> siDependencyList = new ArrayList<>();

			FileReader reader = new FileReader(
					new File(getClass().getClassLoader().getResource("muledependencies.properties").toURI()));
			//FileReader reader = new FileReader(
				//	"D:\\TempMule\\project-creater\\src\\main\\resources\\muledependencies.properties");

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
			writeDependenciestoDestnPOM(siDependencyList);

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeDependenciestoDestnPOM(ArrayList<String> siDependencyList)
			throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String destnPOMLocation = getDirectoryNameForFile(destnProjectLocation,"pom.xml");
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
			//transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transf.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(document);

			StreamResult console = new StreamResult(System.out);
			StreamResult file = new StreamResult(myFile);

			transf.transform(source, console);
			transf.transform(source, file);
		} catch (Exception e) {
			// TODO: handle exception
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
	    for(File f:files) {
	        if(f.isDirectory()) {
	            String loc = getDirectoryNameForFile(f.getPath(), fileName);
	            if(loc != null)
	                return loc;
	        }
	        if(f.getName().equals(fileName))
	            return f.getPath();
	    }
	    return null;
	}


}
