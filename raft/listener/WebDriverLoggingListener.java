package raft.listener;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.events.AbstractWebDriverEventListener;
import org.testng.ITestResult;


import raft.engine.TestEngine;
import raft.util.LoadPara;

/**
 * Logging WebDriver's actions. Care before/afterNavigateTo, before/afterClickOn, 
 * before/afterChangeValueOf, beforeFindBy,   onException actions.
 *
 */
public class WebDriverLoggingListener extends AbstractWebDriverEventListener {
	
	private WebDriver driver;
	private raft.util.WebDriverPlus driverPlus;
	public static SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private PrintWriter logger;
	private ITestResult tr;
	long startTime;
	private static TestMethodStatusListener tmsl;
	
	public static TestMethodStatusListener getTmsl() {
		return tmsl;
	}
	public static void setTmsl(TestMethodStatusListener tmsl) {
		WebDriverLoggingListener.tmsl = tmsl;
	}
	public WebDriver getDriver() {
		return driver;
	}
	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}
	public raft.util.WebDriverPlus getDriverPlus() {
		return driverPlus;
	}
	public void setDriverPlus(raft.util.WebDriverPlus driverPlus) {
		this.driverPlus = driverPlus;
	}
	public void setLogger(PrintWriter logger) {
		this.logger = logger;
	}
	public PrintWriter getLogger(){
		return logger;
	}
	public void setTestResult(ITestResult tr)
	{
		this.tr = tr;
	}
	public WebDriverLoggingListener(TestMethodStatusListener tmsl1)
	{
		setTmsl(tmsl1);
	}
	//current log format --> datetime value: operation: value : duration: value  
	//need log duration: navigate, click, javascript 
	public void beforeNavigateTo(String url, WebDriver driver) {
		logger.println(">>>");
		logger.println(logDateFormat.format(new Date()) +	": navigate to your input url: " + url);
		startTime = System.currentTimeMillis(); //update start time
    }
	public void afterNavigateTo(String url, WebDriver driver) {
		logger.print(logDateFormat.format(new Date()) +	": after navigation,current url goto: <" + driver.getCurrentUrl() + ">");
		logDuration();
    }
	
	public void beforeClickOn(WebElement element, WebDriver driver) {
		logger.println(">>>");
		logger.println(logDateFormat.format(new Date()) +	": click WebElement(id=\"" + element.getAttribute("id") + "\" or name=\"" + element.getAttribute("name") + "\"), and its visible text: \"" + element.getText() + "\"");
		startTime = System.currentTimeMillis(); //update start time
    }
	public void afterClickOn(WebElement element, WebDriver driver) {
		logger.print(logDateFormat.format(new Date()) +	": after clicking on element,current Page url goto: <" + driver.getCurrentUrl() + ">");
		logDuration();
    }
	
	//clear(), sendKeys(), toggle()
	public void beforeChangeValueOf(WebElement element, WebDriver driver) {
		logger.println(">>>");
		if (!element.getValue().isEmpty())
			logger.println(logDateFormat.format(new Date()) +	": WebElement (id=\"" + element.getAttribute("id") + "\" or name=\"" + element.getAttribute("name") + "\") value before changing: \"" + element.getValue() + "\"");
		else
			logger.println(logDateFormat.format(new Date()) +	": WebElement (id=\"" + element.getAttribute("id") + "\" or name=\"" + element.getAttribute("name") + "\") value before changing: Null");
    }
    public void afterChangeValueOf(WebElement element, WebDriver driver) {
    	if (!element.getValue().isEmpty())
    		logger.println(logDateFormat.format(new Date()) +	": WebElement value after changing : \"" + element.getValue() + "\"");
    	else
    		logger.println(logDateFormat.format(new Date()) +	": WebElement value after changing : Null");
    }
	
    //findElement(), findElements()
    public void beforeFindBy(By by, WebElement element, WebDriver driver) {
    	logger.println(">>>");
    	logger.println(logDateFormat.format(new Date()) +	": find element: \"" + by + "\"");
    }
    
    //execute javascript
    public void beforeScript(String script, WebDriver driver) {
    	logger.println(">>>");
    	logger.println(logDateFormat.format(new Date()) +	": Executing javascript: \"" + script + "\"");
		startTime = System.currentTimeMillis(); //update start time
    }
    public void afterScript(String script, WebDriver driver) {
    	logger.print(logDateFormat.format(new Date()) +	": after Executing javascript,current Page url: <" + driver.getCurrentUrl() + ">");
		logDuration();
    }
    
    
    
	//take screen shot when occur Exception
	public void onException(Throwable throwable, WebDriver driver) {
		//logger.print(logDateFormat.format(new Date()) +	": exception: ");
		//if( throwable != null ) throwable.printStackTrace(logger); //detail info
		//if( throwable != null ) logger.println(throwable.getMessage()); //message can be null
		
		if( "false".equals(LoadPara.getGlobalParam("onExceptionScreenshot")) ) {
			return ;
		}
		getTmsl().getMethodErrorSetting().add(tr);
		takeScreenshot(driver, logger, tr.getMethod() + "_TestError",tr);	
    }
	
	private void logDuration() {
		long duration = System.currentTimeMillis() - startTime;
		String respStr = " [Performance Timing]: " + duration + "ms";
		if( duration < 6000 ) ;
		else if( duration >= 6000 && duration < 10000)
			respStr += " (slow)";
		else if( duration >= 10000 && duration < 30000 )
			respStr += " (slower)";
		else if( duration > 30000 )
			respStr += " (unacceptable!)";
		
		logger.println(respStr);
	}
	
	
	/**
	 * Take screenshot. first consider to take existent browser screenshot.
	 * 
	 * @param driver WebDriver instance, if be null, just no screenshot captured
	 * @param logger logging printer, if be null, no logs written
	 * @param fileNamePrefix screenshot file name prefix string, if be null, trans to ""
	 * @param method 
	 * 
	 */
	public static void takeScreenshot(WebDriver driver, PrintWriter logger, String fileNamePrefix, ITestResult tr) {
		File pngFile = new File(TestEngine.getScreenshotRootdir() == null ? System.getProperty("user.dir") : TestEngine.getScreenshotRootdir(), 
				(fileNamePrefix == null ? "" : fileNamePrefix) + new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss_SSS").format(new Date()) + ".png"); 
		File tmpFile = null;
		
		if( driver instanceof InternetExplorerDriver ) {
			tmpFile = ((InternetExplorerDriver)driver).getScreenshotAs(OutputType.FILE);
		} else if( driver instanceof FirefoxDriver ) {
			tmpFile = ((FirefoxDriver)driver).getScreenshotAs(OutputType.FILE);
		} else if( driver instanceof ChromeDriver ) {
			tmpFile = ((ChromeDriver)driver).getScreenshotAs(OutputType.FILE);
		} else {
			System.out.println(logDateFormat.format(new Date()) + " Browser type: " + LoadPara.getGlobalParam("browser") + " do not support screenshot function.");
		}
		try {
			if( tmpFile != null ) {
				FileUtils.copyFile(tmpFile, pngFile);
				if( logger != null )
					logger.println(logDateFormat.format(new Date()) + ": " + fileNamePrefix + " screenshot file: " + pngFile.getAbsolutePath());				
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
		getTmsl().getScreenshotAddressMapping().put(tr, pngFile.getAbsolutePath());
	}

}

