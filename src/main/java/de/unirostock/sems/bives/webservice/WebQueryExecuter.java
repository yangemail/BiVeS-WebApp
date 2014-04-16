/**
 * 
 */
package de.unirostock.sems.bives.webservice;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.binfalse.bflog.LOGGER;
import de.binfalse.bfutils.FileRetriever;
import de.binfalse.bfutils.GeneralTools;
import de.unirostock.sems.bives.Executer;



/**
 * Execute a web query using BiVeS.
 * 
 * @author martin
 * 
 */
public class WebQueryExecuter
{
	
	/** JSON key carrying the files. */
	public static final String	REQ_FILES		= "files";
	
	/** JSON key carrying the requests. */
	public static final String	REQ_WANT		= "commands";
	
	/** Pattern to distinguish xml files from URLs */
	public static final Pattern	XML_PATTERN	= Pattern.compile ("^\\s*<.*",
																						Pattern.DOTALL);
	
	public static final String NEWLINE = System.getProperty("line.separator");
	
	/** The executer. */
	private Executer						exe;
	
	
	/**
	 * Instantiates a new web query executer.
	 */
	public WebQueryExecuter ()
	{
		// we don't want to use local files.
		FileRetriever.FIND_LOCAL = false;
		exe = new Executer ();
	}
	
	
	/**
	 * Execute a query.
	 * 
	 * @param jObj
	 *          the JSON object containing the request
	 * @param toReturn
	 *          the object to store the results
	 * @param err
	 *          the object to store errors
	 * @throws Exception
	 *           the exception
	 */
	@SuppressWarnings("unchecked")
	public void executeQuery (JSONObject jObj, JSONObject toReturn, JSONArray err)
		throws Exception
	{
		// check what we have to do
		int wanted = 0;
		JSONArray jArr = (JSONArray) jObj.get (REQ_WANT);
		LOGGER.setMinLevel (LOGGER.ERROR);
		for (int i = 0; i < jArr.size (); i++)
		{
			if (((String) jArr.get (i)).equals ("verbose"))
			{
				LOGGER.setMinLevel (LOGGER.INFO);
				LOGGER.info ("set info");
				continue;
			}

			if (((String) jArr.get (i)).equals ("stacktrace"))
			{
				LOGGER.setLogStackTrace (true);;
				LOGGER.info ("set stack trace");
				continue;
			}
			
			Executer.Option o = exe.get ((String) jArr.get (i));
			if (o == null)
				throw new IllegalArgumentException ("don't understand option: "
					+ jArr.get (i));
			wanted |= o.value;
		}
		
		// which files to use?
		jArr = (JSONArray) jObj.get (REQ_FILES);
		LOGGER.info ("following files to be used: " + jArr);
		
		// some general checks
		if (jArr.size () < 1)
			throw new IllegalArgumentException ("found no files.");
		if (jArr.size () > 2)
			throw new IllegalArgumentException (
				"found more than 2 files, not supported.");
		
		if (wanted < 1)
			throw new IllegalArgumentException (
				"nothing to do. (no options provided?)");

    List<Exception> errors = new ArrayList<Exception> ();
		// single or compare mode?
    try
    {
			if (jArr.size () == 1)
				exe.executeSingle ((String) jArr.get (0), toReturn, wanted, errors);
			else
				exe.executeCompare ((String) jArr.get (0), (String) jArr.get (1), toReturn,
					wanted, errors);
    }
    catch (Exception e)
    {
    	err.add ("cannot execute: " + e);
    	throw e;
    }
    
  	for (Exception e : errors)
  		err.add ("ERROR: " + e);
		
		// done...
	}
	
	
	/**
	 * Prepares the usage including an example.
	 *
	 * @param request the servlet request
	 */
	public final void usage (HttpServletRequest request)
	{
		
		HashMap<String, Executer.Option> options = exe.getOptions ();
		HashMap<String, Executer.Option> addOptions = exe.getAddOptions ();
		
		StringBuilder str = new StringBuilder (NEWLINE);
		
		SortedSet<String> keys = new TreeSet<String> (options.keySet ());
		int longest = 0;
		for (String key : keys)
		{
			if (key.length () > longest)
				longest = key.length ();
		}
		SortedSet<String> addKeys = new TreeSet<String> (addOptions.keySet ());
		for (String key : addKeys)
		{
			if (key.length () > longest)
				longest = key.length ();
		}
		

		Map<String, String> server = new HashMap<String, String> ();
		server.put ("verbose", "write more information to the log files");
		server.put ("stacktrace", "also log stack traces");
		for (String key : server.keySet ())
		{
			if (key.length () > longest)
				longest = key.length ();
		}
		
		longest += 2;
		
		
		str.append ("COMPARISON COMMANDS").append (NEWLINE);
		
		for (String key : keys)
			str.append ("\t")
			.append (key)
			.append (GeneralTools.repeat (" ", longest - key.length ()))
			.append (options.get (key).description)
			.append (NEWLINE);
		str.append (NEWLINE);
		
		str.append ("ADDITIONAL COMMANDS for single files").append (NEWLINE);
		for (String key : addKeys)
		{
			str.append ("\t")
			.append (key)
			.append (GeneralTools.repeat (" ", longest - key.length ()))
			.append (addOptions.get (key).description)
			.append (NEWLINE);
		}
		str.append (NEWLINE);

		str.append ("SERVER COMMANDS").append (NEWLINE);
		
		for (String key : server.keySet ())
		{
			str.append ("\t")
			.append (key)
			.append (GeneralTools.repeat (" ", longest - key.length ()))
			.append (server.get (key))
			.append (NEWLINE);
		}
		
		
		request.setAttribute ("commands", str.toString ());
	}
}
