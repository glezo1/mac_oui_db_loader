package main;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;

import com.glezo.configurationFileReader.ConfigurationFileReader;
import com.glezo.jWget.JWget;
import com.glezo.stringUtils.StringUtils;

public class Main 
{
	public static void main(String args[])
	{
		String usage="Usage: mac_oui_loader.jar <configuration_file_path>\n";
		usage+="	configuration_file_path  must be in the shape of one line per argument, like <arg_name>=<arg_value>\n";
		usage+="	Mandatory arguments are db_host, db_port, db_user, db_pass.";
		if(args.length!=1)
		{
			System.out.println(usage);
			System.exit(1);
		}
		ConfigurationFileReader cfr=null;
		try 
		{
			cfr = new ConfigurationFileReader(args[0]);
		} 
		catch (FileNotFoundException e)	{System.out.println("Configuration file "+args[1]+" not found");System.exit(1);							}
		catch (ParseException e)		{System.out.println("Parse error at configuration file "+args[1]+e.getMessage());System.exit(1);		}
		catch (IOException e)			{System.out.println("IOException at reading configuration file "+args[1]+e.getMessage());System.exit(1);}
		boolean args_error=false;
		String db_host=null;
		String db_port=null;
		String db_user=null;
		String db_pass=null;
		if(!cfr.exists_variable("db_host"))	{args_error=true;System.out.println("Missing variable db_host in configuration file "+args[1]);}else{db_host=cfr.get_variable_value("db_host");}
		if(!cfr.exists_variable("db_port"))	{args_error=true;System.out.println("Missing variable db_port in configuration file "+args[1]);}else{db_port=cfr.get_variable_value("db_port");}
		if(!cfr.exists_variable("db_user"))	{args_error=true;System.out.println("Missing variable db_user in configuration file "+args[1]);}else{db_user=cfr.get_variable_value("db_user");}
		if(!cfr.exists_variable("db_pass"))	{args_error=true;System.out.println("Missing variable db_pass in configuration file "+args[1]);}else{db_pass=cfr.get_variable_value("db_pass");}
		if(args_error)
		{
			System.out.println(usage);
			System.exit(1);
		}

		String mac_ieee_url="http://standards-oui.ieee.org/oui.txt";
		ArrayList<Byte> oui_bytes=null;
		try 
		{
			oui_bytes=JWget.jwGet(mac_ieee_url);
		} 
		catch (IOException e)	{System.out.println("IOException at reading "+mac_ieee_url);System.exit(2);}
		ArrayList<String> oui_lines=new ArrayList<String>();
		String acum="";
		for(int i=0;i<oui_bytes.size();i++)
		{
			byte current_byte=oui_bytes.get(i);
			char c=(char)current_byte;
			if(c=='\n')
			{
				oui_lines.add(acum);
				acum="";
			}
			else if(c!='\r')
			{
				acum+=c;
			}
		}
		
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Class sun.jdbc.odbc.JdbcOdbcDriver not found exception!");
			System.exit(2);
		}
		Connection con = null;
		Statement statement = null;
		String query="";
		try
		{
			con=DriverManager.getConnection("jdbc:mysql://"+db_host+":"+db_port+"/mac_vendor?user="+db_user+"&password="+db_pass);										
			for( SQLWarning warn = con.getWarnings(); warn != null; warn = warn.getNextWarning() )
			{
				System.out.println( "SQL Warning:" ) ;
				System.out.println( "State  : " + warn.getSQLState()  ) ;
				System.out.println( "Message: " + warn.getMessage()   ) ;
				System.out.println( "Error  : " + warn.getErrorCode() ) ;
			}
			statement = con.createStatement();
		}
		catch(SQLException e)
		{
			System.out.println("SLQ exception! when executing:");
			System.out.println(query);
			while( e != null )
			{
				System.out.println( "State  : " + e.getSQLState()  ) ;
				System.out.println( "Message: " + e.getMessage()   ) ;
				System.out.println( "Error  : " + e.getErrorCode() ) ;
				e = e.getNextException() ;
			}
			System.exit(2);
		}

		try
		{
			for(int i=0;i<oui_lines.size();i++)
			{
				String line=oui_lines.get(i);
				if(line.contains("(hex)"))
				{
					String tokens[]=line.split("\t");
					if(tokens.length>=2) //0,1,2
					{
						String mac=line.substring(0,8);
						String vendor="";
						for(int j=2;j<tokens.length;j++)
						{
							vendor+=tokens[j];
							if(j!=tokens.length-1)
							{
								vendor+=" ";
							}
						}
						vendor=StringUtils.true_trim(vendor);
						query="INSERT IGNORE INTO vendors (mac,vendor,vendor_group) VALUES ('"+mac.replace('-',':')+"','"+vendor.replace("'","''")+"',null)";
						statement.execute(query);
					}
				}
			}
			System.out.println("DONE INSERTING. CREATING GROUPS");
			statement.execute("			UPDATE vendors SET vendor_group='Huawei'		WHERE vendor LIKE '%Huawei%';");
			statement.execute("			UPDATE vendors SET vendor_group='Xerox'			WHERE vendor LIKE '%Xerox%';");
			statement.execute("			UPDATE vendors SET vendor_group='Fuji-Xerox'	WHERE vendor LIKE '%Fuji%Xerox%';");
			statement.execute("			UPDATE vendors SET vendor_group='Guandong'		WHERE vendor LIKE '%Guandong%';");
			statement.execute("			UPDATE vendors SET vendor_group='Hon Hai'		WHERE vendor LIKE '%Hon Hai%';");
			statement.execute("			UPDATE vendors SET vendor_group='Cisco'			WHERE vendor LIKE '%Cisco%';");
			statement.execute("			UPDATE vendors SET vendor_group='Linksys'		WHERE vendor LIKE '%Linksys%';");
			statement.execute("			UPDATE vendors SET vendor_group='Samsung'		WHERE vendor LIKE '%Samsung%' AND vendor NOT LIKE '%Toshiba%';");
			statement.execute("			UPDATE vendors SET vendor_group='Intel'			WHERE vendor LIKE '%Intel Corp%';");
			statement.execute("			UPDATE vendors SET vendor_group='HP'			WHERE vendor LIKE '%Hewlett%Packard%';");
			statement.execute("			UPDATE vendors SET vendor_group='Apple'			WHERE vendor LIKE '%Apple%';");
			statement.execute("			UPDATE vendors SET vendor_group='Microsoft'		WHERE vendor LIKE '%Microsoft%';");
			statement.execute("			UPDATE vendors SET vendor_group='ASUSTek'		WHERE vendor LIKE '%ASUSTek%';");
			statement.execute("			UPDATE vendors SET vendor_group='AsRock'		WHERE vendor LIKE '%AsRock%';");
			statement.execute("			UPDATE vendors SET vendor_group='Dell'			WHERE vendor LIKE '%Dell %' OR vendor='Dell';");
			statement.execute("			UPDATE vendors SET vendor_group='Giga-byte'		WHERE vendor LIKE '%Giga-Byte%';");
			statement.execute("			UPDATE vendors SET vendor_group='D-link'		WHERE vendor LIKE '%D-link%';");
			statement.execute("			UPDATE vendors SET vendor_group='Sony'			WHERE vendor LIKE '%Sony %' AND vendor NOT LIKE '%Ericsson%';");
			statement.execute("			UPDATE vendors SET vendor_group='Sony Ericsson'	WHERE vendor LIKE '%Sony%Ericsson%';");
			statement.execute("			UPDATE vendors SET vendor_group='ZyXEL'			WHERE vendor LIKE '%ZyXEL Communications Corporation%';");
			statement.execute("			UPDATE vendors SET vendor_group='ZyGate'		WHERE vendor LIKE '%ZyGate Communications, Inc.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Bell'			WHERE vendor LIKE '%BELL TECHNOLOGIES%';");
			statement.execute("			UPDATE vendors SET vendor_group='NORMEREL'		WHERE vendor LIKE '%NORMEREL SYSTEMES%';");
			statement.execute("			UPDATE vendors SET vendor_group='TP-LINK'		WHERE vendor LIKE '%TP-LINK TECHNOLOGIES CO.,LTD.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Quanta'		WHERE vendor LIKE '%Quanta Computer Inc.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Quanta'		WHERE vendor LIKE '%Quanta Microsystems, INC.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Azurewave'		WHERE vendor LIKE '%Azurewave Technologies%';");
			statement.execute("			UPDATE vendors SET vendor_group='Liteon'		WHERE vendor LIKE '%Liteon Technology Corporation%';");
			statement.execute("			UPDATE vendors SET vendor_group='Elitegroup'	WHERE vendor LIKE '%Elitegroup Computer System%';");
			statement.execute("			UPDATE vendors SET vendor_group='Belkin'		WHERE vendor LIKE '%Belkin International Inc.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Realtek'		WHERE vendor LIKE '%REALTEK SEMICONDUCTOR CORP.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Asiarock'		WHERE vendor LIKE '%Asiarock Technology Limited%';");
			statement.execute("			UPDATE vendors SET vendor_group='Asiarock'		WHERE vendor LIKE '%Asiarock Incorporation%';");
			statement.execute("			UPDATE vendors SET vendor_group='VMware'		WHERE vendor LIKE '%VMware, Inc.%';");
			statement.execute("			UPDATE vendors SET vendor_group='IBM'			WHERE vendor LIKE '%IBM Corp%';");
			statement.execute("			UPDATE vendors SET vendor_group='Compal'		WHERE vendor LIKE '%Compal Electronics, Inc.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Compal'		WHERE vendor LIKE '%COMPAL INFORMATION (KUNSHAN) CO., LTD.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Ayecom'		WHERE vendor LIKE '%Ayecom Technology Co., Ltd.%';");
			statement.execute("			UPDATE vendors SET vendor_group='ADB'			WHERE vendor LIKE '%ADB Broadband Italia%';");
			statement.execute("			UPDATE vendors SET vendor_group='Nokia'			WHERE vendor LIKE '%Nokia Corporation%';");
			statement.execute("			UPDATE vendors SET vendor_group='Murata'		WHERE vendor LIKE '%Murata Manufacturing Co., Ltd.%';");
			statement.execute("			UPDATE vendors SET vendor_group='Motorola'		WHERE vendor LIKE '%Motorola Mobility LLC%';");
						
			System.out.println("DONE!");
			statement.close();
			con.close();	
		}
		catch(SQLException e)
		{
			System.out.println("SLQ exception! when executing:");
			System.out.println(query);
			while( e != null )
			{
				System.out.println( "State  : " + e.getSQLState()  ) ;
				System.out.println( "Message: " + e.getMessage()   ) ;
				System.out.println( "Error  : " + e.getErrorCode() ) ;
				e = e.getNextException() ;
			}
			System.exit(2);
		}
		
	}
}
