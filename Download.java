package com.nitdgp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Download
{
	public static final int BUFFER_SIZE = 1024;
	String file_url;
	File out_file_part;
	Long file_size;
	int num_of_files;

	public Download(String url, File part, Long size, int num) 
	{
		file_url = url;
		out_file_part = part;
		file_size = size;
		num_of_files = num;
	}

	private void renameFile() 
	{
		/*
		get the complete path
		*/
		String path = out_file_part.getAbsolutePath();
		/*
		remove the appended .part form the pathname
		*/
		String new_path = path.substring(0, path.length() - ".part".length());
		File renamed_file = new File(new_path);

		/* rename the file */
		if(out_file_part.renameTo(renamed_file) == false) {
			System.out.println("Some error ocurred while saving file.");
		}
	}

	public int run() 
	{
		try {
			/*
			instantiate the file url and setup an HTTP connection to this URL.
			get the input stream for the http connection and intantiate a buffered
			input stream, in order to read from the URL in a buffer.
			*/
			URL url = new URL(file_url);
			HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();

			/* starting the connection */
			//http_conn.connect();

			BufferedInputStream in_file = new BufferedInputStream(http_conn.getInputStream());

			/* 
			Set up the output stream in order to write into a local file, 
			whatever is read from the HTTP connection.
			*/
			FileOutputStream out_file;

			/*
			Strategy for resuming file download:
			if the part file argument received to this class already exists then
			it means, earlier the download was interrupted and we need to append to the
			file, exactly from the byte number, at which download was interrupted.
			*/
			int read = 0;
			long downloaded = 0;
			int download_percentage = 0;

			if(out_file_part.exists()) 
			{
				out_file = new FileOutputStream(out_file_part, true); /* append mode true */
				downloaded = out_file_part.length();
			}
			else 
			{
				out_file = new FileOutputStream(out_file_part);
			}

			/*
			we skip the input stream (from where we are reading), to the last written byte in the file. 
			It keeps the file pointer at the beginning if there is no earlier content in the file.
			*/

			in_file.skip(out_file_part.length());

			/*
			We create a buffered output stream, to write to file in the system, 
			what we are reading from the buffered input stream.
			*/

			BufferedOutputStream out_file_buffered = new BufferedOutputStream(out_file, 
				Download.BUFFER_SIZE);
			byte [] buffer = new byte[Download.BUFFER_SIZE];
			/*
			read the input stream in buffer and write the buffer to the
			file in the system.
			*/
			read = in_file.read(buffer);
			while(read != -1) 
			{
				out_file_buffered.write(buffer, 0, read);
				downloaded += read;
				download_percentage = (int) (((double)downloaded/file_size) * 100);
				System.out.print("\b\b\b" + download_percentage + "%");

				read = in_file.read(buffer);
			}
			//this.renameFile();
			System.out.println();
			in_file.close();
			out_file_buffered.close();
			if(downloaded == file_size) 
			{
				System.out.println("File Synced successfully.");
				System.out.println();
				/* rename the file and remove the appended ".part" from the file */
				this.renameFile();
				return 1;
			}
			else 
			{
				System.out.println("Some error ocurred while downloading.");
				System.out.println();
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}	

}