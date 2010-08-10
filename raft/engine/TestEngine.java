package raft.engine;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.testng.TestNG;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import raft.listener.TestMethodStatusListener;
import raft.listener.TestReportListener;
import raft.util.XmlUtil;

/* it is developped by StarCite Engineering team @2010/07*/

public class TestEngine {
	private static String classesRootdir;
	private static String reportRootdir;
	private static String loggerRootdir;
	private static String screenshotRootdir;
	private static Map<String, String> globalMap;
	private static TestMethodStatusListener testMethodStatusListener;
	
	public static String getClassesRootdir() {
		return classesRootdir;
	}
	public static String getReportRootdir() {
		return reportRootdir;
	}
	public static String getLoggerRootdir() {
		return loggerRootdir;
	}
	public static String getScreenshotRootdir() {
		return screenshotRootdir;
	}
	public static Map<String, String> getGlobalMap() {
		return globalMap;
	}
	public static TestMethodStatusListener getTestMethodStatusListener() {
		return testMethodStatusListener;
	}
	
	private static final String fileSep = File.separator;
	
	
	public static void main(String args[]) throws Exception {
		
		
		if(args.length > 0 && args[0].equals("help")) {
			System.out.println("Usage: java TestEngine [TestClassesFolder] [globalConfigFile] [testngFile] [reportFolder]");
			return ;
		}
		long startTime = System.currentTimeMillis();
		
		//default: "Classes" folder
		classesRootdir = args.length > 0 ? args[0] + fileSep : ".." + fileSep + "Classes" + fileSep;
		
		//default: "globalPara.xml" under classes folder
		String globalParaFile = args.length > 1 ? args[1] : classesRootdir + "globalPara.xml";
		
		//default: "Config.xml" under current folder
		String testngFile = args.length > 2 ? args[2] : "Config.xml";

		//default: "Report_XXX", under current folder
		reportRootdir = args.length > 3 ? args[3] + fileSep + "Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + fileSep
				: "Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + fileSep;
		
		//create logs folder, "logs" folder under report folder
		new File(loggerRootdir = reportRootdir + "logs").mkdirs();
		
		//create screenshots folder, "screenshots" folder under report folder
		new File(screenshotRootdir = reportRootdir + "screenshots").mkdirs();
		
		System.out.println("\n=================run tests=================");
		globalMap = XmlUtil.readXmlToMap(globalParaFile, "//var", "name");
		handleTestNGRun(testngFile); //call testng
		outputTimeConsumption(System.currentTimeMillis(), startTime);
	}

	/**
	 * Output the time consumption statistics from start to end. 
	 * @param end end millisecond
	 * @param start start millisecond
	 * @return time consumption statistics string
	 */
	public static String outputTimeConsumption(long end, long start) {
		
		long diff = end - start;
		long day = diff/(24*3600000);
		long hour = diff/3600000 - day*24;
		long minute = diff/60000 - day*24*60 - hour*60;
		long second = diff/1000 - day*24*3600 - hour*3600 - minute*60;
		String totalTimeStr = day + "d " + 
		hour+ "h " + 
		minute + "m " + 
		second + "s";
		//System.out.println("FrameworkRunner time consumption : " + totalTimeStr );
		return totalTimeStr;
	}
	/**
	 * Output the testMethod duration. 
	 * @param end end millisecond
	 * @param start start millisecond
	 * @return duration statistics string
	 */
	public static String duration(long end ,long start)
	{
		long diff = end - start;
		long minute = diff/60000;
		long second = diff/1000 - minute*60;
		String min = minute<10?"0"+minute:minute+"";
		String sec = second<10?"0"+second:second+"";
		String dur = min+":"+sec;
		return dur;
	}
	/**
	 * Call testng to run test cases.
	 * @param testngFile testng running configuration file, such as "testng.xml", "config1.xml,config2.xml"
	 * @throws Exception
	 */
	private static void handleTestNGRun(String testngFile) throws Exception {
		TestNG testng = new TestNG();
		List<XmlSuite> allXmlSuites = new ArrayList<XmlSuite>();
		for (String suiteFile : testngFile.split(",")) {
			Collection<XmlSuite> allSuites = new Parser(suiteFile).parse();  //suiteFile itself has "/testng.xml"
			for (XmlSuite s : allSuites) {
				allXmlSuites.add(s);
				for( XmlTest test : s.getTests() ) { //<test> tag
					if( test.getParameter("testngFile") != null )  //if there is a "testngFile" parameter under <test> tag, parse it to a 
						allXmlSuites.addAll( new Parser(test.getParameter("testngFile")).parse() ); //testng configuration file and run it.
				}
		    }
		}
		
		int globalRunCount = 1; //default
		if( globalMap.get("globalRunCount") != null && globalMap.get("globalRunCount").trim().length() != 0 )
			globalRunCount = Integer.valueOf(globalMap.get("globalRunCount"));
		if( globalRunCount == 0 ) allXmlSuites.clear();
		else if( globalRunCount == 1 ) ;
		else {
			List<XmlSuite> allXmlSuitesOriginal = new ArrayList<XmlSuite>(allXmlSuites);
			for(int i=2; i<=globalRunCount; i++) {  //notice: if use testng default html report only can recognize one suite if more suites have the same names
				allXmlSuites.addAll(allXmlSuitesOriginal);  //though changed the suite name. html report still not ok. but testng-results.xml has the correct values.
			}
		}
		
		testng.setXmlSuites(allXmlSuites); 
		testng.setOutputDirectory(reportRootdir);
		testng.setUseDefaultListeners(false);
		
		//add TestNG listeners
		testMethodStatusListener = new TestMethodStatusListener(); //enable obtained by forward operation
		testng.addListener(testMethodStatusListener);
		testng.addListener(new TestReportListener(testMethodStatusListener));
		
		testng.run();
	}

}
