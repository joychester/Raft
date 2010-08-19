package raft.listener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.WebDriver;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import raft.engine.TestEngine;
import raft.util.LoadPara;
import raft.util.WebDriverBasic;
import raft.util.XmlUtil;

/**
 * ITestListener, to listen the test method's status.
 * 
 * @author james.deng
 *
 */
public class TestMethodStatusListener extends TestListenerAdapter {
	//to record which test methods are Error! 
	private  Set<ITestResult> methodErrorSetting = new HashSet<ITestResult>();
	//to record screenshot picture's absolute path
	private Map<ITestResult,String> screenshotAddressMapping = new HashMap<ITestResult,String>();
	
	
	
	public Set<ITestResult> getMethodErrorSetting() {
		return methodErrorSetting;
	}
	public Map<ITestResult, String> getScreenshotAddressMapping() {
		return screenshotAddressMapping;
	}

	public void setScreenshotAddressMapping(
			Map<ITestResult, String> screenshotAddressMapping) {
		this.screenshotAddressMapping = screenshotAddressMapping;
	}



	static Map<Method, WebDriverLoggingListener> methodWDLListenerMapping = new HashMap<Method, WebDriverLoggingListener>();
	public WebDriverLoggingListener getWebDriverLoggingListener(Method method) {  //called by GetWebDriverPlus.getLoggingWebDriver(), register this instance.
		return methodWDLListenerMapping.get(method);
	}
	/**
	 * New a WebDriverLoggingListener instance when test method start and update ITestResult info.
	 * 
	 * createLogger, startDaemonThread, load local parameters(load once for every test class), ...
	 * 
	 */
	synchronized public void onTestStart(ITestResult result) {
		//System.out.println("parallel level: " + result.)
		WebDriverLoggingListener wdlListener = new WebDriverLoggingListener(this); //create an instance for every test method
		result.setAttribute("wdlListener", wdlListener);
		methodWDLListenerMapping.put(result.getMethod().getMethod(), wdlListener); //method <--> WDLListener mapping
		
		try {
			createLogger(result.getMethod(), true);
			wdlListener.setLogger(methodLoggerMapping.get(result.getMethod())); //notify logger
			//wdlListener.setTestMethod(result.getMethod());  //notify testMethod
			wdlListener.setTestResult(result);
			startDaemonThread(result.getMethod());
			
			Class<?> clazz = result.getMethod().getRealClass();
			//load one map for one test class
			if( classMapMapping.get(clazz) == null ) {
				String className = clazz.getName();
				String localParaDir = TestEngine.getClassesRootdir(); //if no package name, put .class, .xml files under the same folder
				if( className.indexOf(".") !=- 1 ) {
					String strArr[] = className.split("\\.");
					localParaDir = "";
					for(int i=0; i<strArr.length-2; i++)
						localParaDir += strArr[i] + File.separator;
					localParaDir = TestEngine.getClassesRootdir() + localParaDir + strArr[strArr.length-2] + "_para" + File.separator; //if has ".", at least be split two parts. 
				}
				String localParaFile = localParaDir + className.substring(className.lastIndexOf(".")+1, className.length()) + ".xml";
				
				if( !new File(localParaFile).exists() ) {System.out.println("No local parameter file: " + localParaFile + " found."); return ; }
				
				System.out.println("Loading local parameters(" + localParaFile + ") ...");
				classMapMapping.put(clazz, XmlUtil.readXmlToMap(localParaFile, "//var", "name")); //test class <--> xml map data mapping
			}
		}
		catch( Exception e ) { e.printStackTrace(); throw new RuntimeException(e); }
	}
	
	//close the test method's logger, and kill browser if user defined.
	public void onTestSuccess(ITestResult tr) {
		java.io.PrintWriter logger = getMethodLoggerMapping().get(tr.getMethod());
		logger.println(WebDriverLoggingListener.logDateFormat.format(new java.util.Date()) + ": "+tr.getMethod().getRealClass().getName()+"."+tr.getMethod().getMethodName()+" Method Success!"); 
		finishTestMethod(tr);
	}
	public void onTestSkipped(ITestResult tr) {
		
		System.out.println(tr.getMethod().getMethodName()+"SKIPES");
		//finishTestMethod(tr);
	}
	public void onTestFailedButWithinSuccessPercentage(ITestResult tr) {
		finishTestMethod(tr);
	}
	
	//take browser page shot if test failed.
	public void onTestFailure(ITestResult tr) {
		java.io.PrintWriter logger = getMethodLoggerMapping().get(tr.getMethod());
		if( tr.getThrowable() != null ) {
			logger.print(WebDriverLoggingListener.logDateFormat.format(new java.util.Date()) +(IsError(tr.getMethod())?":test method have errors:":": test method failed: ") ); 
			tr.getThrowable().printStackTrace(logger);
		}
			
		
		WebDriver driver = ((WebDriverLoggingListener)tr.getAttribute("wdlListener")).getDriver();
		//WebDriver driver = ((WebDriverLoggingListener)tr.getAttribute("wdlListener")).getDriverPlus().getWrappedDriver(); //can't use it to take scrennshot, I don't know why.
		
		if( "false".equals(LoadPara.getGlobalParam("onAssertionFailScreenshot")) ) {
			finishTestMethod(tr);
			return ;
		}
		
		if( driver == null ) System.out.println(tr.getMethod() + " not used WebDriverPlus to get a WebDriver instance. So, there is no screenshot when fail.");
		else 
		{		
				WebDriverLoggingListener.takeScreenshot(driver, logger, tr.getMethod() + (IsError(tr.getMethod())?"_TestError":"_TestFailed"),tr);	
		}
			
		
		finishTestMethod(tr);
	}

	/**
	 * finishLogger, finish daemon thread, ...
	 * 
	 * @param tr
	 */
	public void finishTestMethod(ITestResult tr) {
		try {
			//if browser was killed by autoBrowserKiller, test method will failed by other WebDriver operation(such 
			//as findElementBy, so the Exception is not be autoBrowserKiller's thrown Exception), 
			//then occur error, then quit invoking method and call afterInvocation method.
			if( methodExceptionMapping.get(tr.getMethod()) != null ) { //if has timeout exception
				tr.setStatus(ITestResult.FAILURE);
				tr.setThrowable(methodExceptionMapping.get(tr.getMethod()));
			} else { //normal quits
				if( methodThreadMapping.get(tr.getMethod()) != null )
					methodThreadMapping.get(tr.getMethod()).interrupt(); //if test method normal quits, interrupt the daemon thread. 
			}
				
			finishLogger(tr.getMethod());
			
			raft.util.WebDriverPlus driverPlus = ((WebDriverLoggingListener)tr.getAttribute("wdlListener")).getDriverPlus(); 
			if(driverPlus != null) driverPlus.quit();
			
			if( "true".equals(LoadPara.getGlobalParam("autoBrowserKiller")) && killBrowserIfExistByBrowserType(LoadPara.getGlobalParam("browser")) )
				System.out.println("Image: " + getImageName(LoadPara.getGlobalParam("browser")) + " was killed by auto browser killer.");
			
		} catch( Exception e ) { e.printStackTrace(); throw new RuntimeException(e); }
	}
	
	/**
	 * Get the image name by browser type string. 
	 * @param browser browser type string
	 * @return respond image name, null if not matched browser type string.
	 */
	public String getImageName(String browser) {
		WebDriverBasic driverAgent = new WebDriverBasic(browser);
		String imageName = null;
		if( driverAgent.isIE() ) imageName="iexplore.exe";
		else if( driverAgent.isFirefox() ) imageName="firefox.exe";
		else if( driverAgent.isChrome() ) imageName="chrome.exe";
		else if( driverAgent.isHtmlUnit() ) ; //"javaw.exe", ignore it.
		
		return imageName;
	}
	public boolean IsError(ITestNGMethod method)
	{
		boolean bool = false;
		for(ITestResult tr:this.getMethodErrorSetting())
		{
			
			if(tr.getMethod().equals(method))
			{
				bool = true;
				break;
			}	
		}
		return bool;
	}
	public boolean isBrowerProcessExist(String imageName) throws Exception {
		return callSystemCmd("TASKLIST /FI \"IMAGENAME eq " + imageName + "\"").contains(imageName); 
	}

	public void killBrowserProcess(String imageName) throws Exception {
		callSystemCmd("TASKKILL /F /IM " + imageName + " /T");
	}
	
	public boolean killBrowserIfExistByBrowserType(String browser) throws Exception {
		boolean killed = false;
		String imageName = getImageName(browser); 
		if( imageName != null && isBrowerProcessExist(imageName) ) {
			killBrowserProcess(imageName);
			killed = true;
		}
		return killed;
	}
	
	public String callSystemCmd(String cmdLine) throws Exception {
		return pToConsole(Runtime.getRuntime().exec(cmdLine));
	}
	
	/**
	 * Return the output stream of a java calling system commands.
	 * @param p calling process
	 * @return string of this process's output stream
	 * @throws Exception
	 */
	public String pToConsole(Process p) throws Exception {
		String line=null;
		BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuffer strBuf = new StringBuffer();
		while((line=br.readLine())!=null) {
			//System.out.println(line);
			strBuf.append(line);
			strBuf.append("\n");
		}
		br.close();
		
		return strBuf.toString();
	}
	


//////////////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * 
	 * A listener that gets invoked before and after a method is invoked by TestNG.
	 * This listener will only be invoked for configuration and test methods.
	 *
	 * @author james.deng
	 *
	 */
	
	//one class, one map(xml para file data)
	static Map<Class<?>, Map<String, String>> classMapMapping = new HashMap<Class<?>, Map<String, String>>();
	//one method, one daemon thread
	static Map<ITestNGMethod, Thread> methodThreadMapping = new HashMap<ITestNGMethod, Thread>();
	//if method killed by autoBrowserKiller, mapping one exception
	static Map<ITestNGMethod, Throwable> methodExceptionMapping = new HashMap<ITestNGMethod, Throwable>();
	//one method, one logger(PrintWriter)
	static Map<ITestNGMethod, PrintWriter> methodLoggerMapping = new HashMap<ITestNGMethod, PrintWriter>();
	//one method, one log file(File)
	static Map<ITestNGMethod, File> methodFileMapping = new HashMap<ITestNGMethod, File>();
	
	public static Map<ITestNGMethod, PrintWriter> getMethodLoggerMapping() {
		return methodLoggerMapping;
	}

	
	/**
	 * Create a logger for every method.
	 * 
	 * @param method test method
	 * @param append append or overwrite
	 * @throws Exception
	 */
	public void createLogger(ITestNGMethod method, boolean append) throws Exception { //called by beforeInvocation, so don't know whether listener be initialized.
		File file = new File(TestEngine.getLoggerRootdir(), "log_" + method + ".txt");
		methodFileMapping.put(method, file);
		methodLoggerMapping.put(method, new PrintWriter(new FileOutputStream(file, append )) );
	}
	
	/**
	 * Flush and close the logger, if file is empty, delete it.
	 * 
	 * @param method test method
	 * @throws Exception
	 */
	public void finishLogger(ITestNGMethod method) throws Exception {
		//if test method status is skipped, won't call onTestStart(), so no logger created.
		if( methodLoggerMapping.get(method) != null ) {
			methodLoggerMapping.get(method).flush();
			methodLoggerMapping.get(method).close();
			if( methodFileMapping.get(method).length() == 0 )
				methodFileMapping.get(method).delete();
		}
	}
	
	
	/**
	 * Start a daemon thread to monitor the browser process.
	 * If the browser process still be present when a global method invoking timeout or 
	 * method ok but user forgot to close/quit the WebDriver, kill the browser process.
	 * 
	 * @param method test method identifier
	 * 
	 */
	public void startDaemonThread(final ITestNGMethod method) {
		Thread thread = new Thread() {
			public void run() {
				try {
					long timeout = 10*60*1000 ; //default 10 minutes
					String browserKillerTimeout = LoadPara.getGlobalParam("browserKillerTimeout");
					if( browserKillerTimeout.trim().length() != 0 )
						timeout = Long.valueOf(browserKillerTimeout);
					
					Thread.sleep(timeout); //sleep until timeout or interrupted
					
					if( killBrowserIfExistByBrowserType(LoadPara.getGlobalParam("browser")) ) {
						String str = "Image: " + getImageName(LoadPara.getGlobalParam("browser")) + 
						" was killed by auto browser killer. Timeout: " + timeout;
						System.out.println(str);
						methodExceptionMapping.put(method, new Exception(str)); //mark the exception if be timeout
					}
					
				} catch(InterruptedException  e) { 
					//just return ;
				} catch (Exception e) {
					e.printStackTrace(); //this new thread's exception, no need to throw.
				} finally {
					methodThreadMapping.put(method, null); //thread over.
				}
			}
		};
		methodThreadMapping.put(method, thread);
		thread.start();
	}


	
	/**
	 * Get parameter value via a className and parameter name
	 * @param className test class name
	 * @param name parameter name
	 * @return parameter value
	 */
    public static String getParam(String className, String name) {
    	Class<?> clazz = null;
    	try {
    		clazz = Class.forName(className);
		} catch(Exception e) { throw new RuntimeException(e); }
		
		return getParam(clazz, name);
    }
    
    /**
     * Get parameter value via a Class object and parameter name
     * @param clazz test class
     * @param name parameter name
     * @return parameter value
     */
	public static String getParam(Class<?> clazz, String name) {
		if(classMapMapping.get(clazz) == null) return "";
		else return classMapMapping.get(clazz).get(name);
	}
}
