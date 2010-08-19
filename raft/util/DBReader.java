package raft.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//Establish a connection to a MSSQL database using JDBC.

public class DBReader {
	
	//Connect to SQL server 2005 for us
	static String constring = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
	
	static Connection conn = null;
	static Statement stmt = null;
	static ResultSet rs = null;
	static List<String> dataset = new ArrayList<String>();

	
	public void Getconnection( String url, String dbname, String pwd){
		
		try
	    {
	      // Step 1: Load the JDBC driver. 
			Class.forName(constring);
			
	      // Step 2: Establish the connection to the database. 
			conn = DriverManager.getConnection(url,dbname,pwd); 
		    
	    }
	    catch (Exception e)
	    {
	      System.err.println("Can not get DB connection from " + url); 
	      System.err.println(e.getMessage()); 
	    }
	  } 
	
	public List<String> Query( String sqlstatement,String coloumnname) throws SQLException{

	    String SQL = sqlstatement;
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery(SQL);
	    
	    while (rs.next()) {
	    	dataset.add(rs.getString(coloumnname));
	    }
	    return dataset;
	}
	
	public void Close(){

         if (rs != null) try { rs.close(); rs= null;} catch(Exception e) {}
         if (stmt != null) try { stmt.close(); stmt= null;} catch(Exception e) {}
         if (conn != null) try { conn.close(); conn=null;} catch(Exception e) {}

	}
}



    
