package raft.util;
import raft.engine.TestEngine;
import raft.listener.TestMethodStatusListener;

/**
 * Load global and local parameters.
 * 
 * it is developped by StarCite Engineering team @2010/07
 *
 */
public class LoadPara {

	/** load global parameter */
	public static String getGlobalParam(String name) {
		return TestEngine.getGlobalMap().get(name);
	}
	
	/** load global parameter, return int type */
	public static int getGlobalParamInt(String name) {
		return Integer.valueOf(TestEngine.getGlobalMap().get(name));
	}
	
	/** load global parameter, return an array.  parameters' split char is specified by "arraySeparator"
	 parameter or default be "," */
	public static String[] getGlobalParamArray(String name) {
		String separator = getGlobalParam("arraySeparator") == null ? "," : getGlobalParam("arraySeparator");
		return getGlobalParam(name).split(separator);
	}
	
	
	/** load local parameter */
	public static String getLocalParam(String name) {
		//Thread.currentThread().getStackTrace()[2] is calling test class' stack trace.
		return TestMethodStatusListener.getParam(Thread.currentThread().getStackTrace()[2].getClassName(), name);
	}
	
	/** load local parameter, return int type */
	public static int getLocalParamInt(String name) {
		//Thread.currentThread().getStackTrace()[2] is calling test class' stack trace.
		return Integer.valueOf(TestMethodStatusListener.getParam(Thread.currentThread().getStackTrace()[2].getClassName(), name));
	}
	
	/** load global parameter, return an array.  parameters' split char is specified by "arraySeparator"
	 parameter under local xml file 
	 or "arraySeparator"  parameter under global xml,
	 or default be "," */
	public static String[] getLocalParamArray(String name) {
		//Thread.currentThread().getStackTrace()[2] is calling test class' stack trace.
		String className = Thread.currentThread().getStackTrace()[2].getClassName();
		//whether exist local "arraySeparator" setting. notice calling class
		String separator = TestMethodStatusListener.getParam(className, "arraySeparator"); 
		if( separator == null || separator.trim().length() == 0 ) {
			separator = getGlobalParam("arraySeparator"); //whether exist global "arraySeparator" setting
			if( separator == null || separator.trim().length() == 0 ) 
				separator = ","; //default
		}
		
		return TestMethodStatusListener.getParam(className, name).split(separator);
	}
	
}
