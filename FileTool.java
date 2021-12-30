package com.nitdgp;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;

public class FileTool 
{

	public static final String LOCAL_DIR = "/home/krishna/Work/fileApp/files";

	Collection local_files;
	Collection remote_files;

	HashMap<String, Long> file_size_map;

	String file_url;
	String download_name;

	String remote_url_string;

	public FileTool(String url) 
	{
		file_url = new String();
		download_name = new String();

		local_files = new ArrayList<String>();
		remote_files = new ArrayList<String>();

		remote_url_string = url;
		file_size_map = new HashMap<>();
	}

	public void syncFiles() throws IOException,
		MalformedURLException 
	{
		/*
		first we get the files on the local machine and store them
		in local_files list
		*/
		this.getLocalFiles();

		/*
		get the remote files and store them in remote_files list
		*/
		this.getRemoteFiles();

		/* getting the files in remote_files list which are not 
		present in local system*/

		Collection diff = new ArrayList<String>(remote_files);
		diff.removeAll(local_files);

		if(diff.size() == 0) 
		{
			System.out.println("\nAll files synced.");
		}
		else 
		{
			this.downloadFiles(diff);
		}
	};

	private void downloadFiles(Collection diff_files) 
	{
		Iterator<String> it = diff_files.iterator();

		System.out.println();
		System.out.println("Files to be synced: " + diff_files);
		System.out.println();

		String file;
		int num_of_files = diff_files.size();
		int count = 0;
		while(it.hasNext())
		{
			file = it.next();
			file_url = remote_url_string + "getfile?f_n=" +  URLEncoder.encode(file);

			/*
			We download the file by appending a string ".part" to the filename.
			This appended string is removed once the download is complete.
			This is being done, because, if the file is partially downloaded
			and has the same name as that of the file on remote server, then it will 
			be removed from the files to be downloaded list. If the name is slightly changed
			while downloading, then it will not be removed in case of partial downloads. 
			*/
			download_name = LOCAL_DIR + "/" + file + ".part"; 

			File out_file = new File(download_name);

			System.out.println("Downloading file: " + file);

			/* getting the size of file */
			Long size = file_size_map.get(file);

			count += new Download(file_url, out_file, size, num_of_files).run();
		}

		if (count == num_of_files) 
		{
			System.out.println("ALL FILES SYNCED.");
		}
	}

	private void getLocalFiles() 
	{
		File dir = new File(LOCAL_DIR);
		System.out.println();

		if(dir.listFiles().length == 0) 
		{
			System.out.println("No files found at local system.");	
		}
		else 
		{
			System.out.println("Files at the local system: \n");
		}

		/* loop through all the files in local dir */
		for(File file: dir.listFiles()) {
			/* push the filenames into the local_files list */
			local_files.add(file.getName());

			System.out.println(file.getName());
		}
	}

	private void getRemoteFiles()  throws IOException,
		MalformedURLException 
	{
		/* instantiate the URL*/
		URL remote_url = new URL(remote_url_string);

		/* 
		open an inputstream at the given URL and instantiate a 
		BufferedReader to read from InputStream line by line
		*/
		BufferedReader reader = new BufferedReader(new 
			InputStreamReader(remote_url.openStream()));

		String line;
		
		/*
		read the given URL stream line by line. Each line at 
		the remote URL contains name of a file residing at 
		remote location.
		*/
		System.out.println();

		/* read first line from server response */
		line = reader.readLine();

		/* if response is null, there are no files at server*/
		if(line == null) 
		{
			System.out.println("No files found at remote server.");
		}
		else 
		{
			System.out.println("File at the remote server: \n");
		}

		while(line != null) 
		{
			/*
			every line of server contains filename and filesize
			separted by \t
			We will split the name and size and store it accordinggly.
			*/
			String [] line_part = line.split("\t"); 
			
			String filename = line_part[0];
			Long filesize = Long.parseLong(line_part[1]);

			/* don't get the files which are partial downloads on the remote server */
			int len = filename.length();
			String sub = filename.substring(len - 5, len);
			if(len < 5 || !sub.equals(".part")) 
			{
				/* adding filename to the list of files */
				remote_files.add(filename);
				System.out.println(filename);

				/* adding filename and size to file_size_map */
				file_size_map.put(filename, filesize);
			}

			line = reader.readLine();
		}

		reader.close();

	}

}