package com.nitdgp;
import java.util.*;
import java.io.*;
import fi.iki.elonen.NanoHTTPD;
import javax.activation.MimetypesFileTypeMap;
import java.net.URLDecoder;
import java.lang.InterruptedException;

public class App 
{
    /* contains the URL of remote server.*/
    public static String REMOTE_URL = "http://192.168.1.103:54321/";

    public static void main( String[] args ) throws IOException, InterruptedException
    {
        WebServer server = new WebServer();

        /* start the local server */
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server started on htttp://localhost:12345");

        if(args.length == 1) {
            REMOTE_URL = "http://"+args[0]+":54321/";
        }

        FileTool tool = new FileTool(REMOTE_URL);

        /* keep trying to connect to remote server, until it connects.*/
        boolean retry =  true;

        while(retry)
        {
            try {

                /*
                We start syncing file with remote server.
                */
                tool.syncFiles();

                retry = false;
            } catch (IOException e) {
                /* if URL to the server is not reachable */
                System.out.println();
                System.out.println("Error: Remote Server not reachable.");
                System.out.println("Retrying in 8 sec.");
                Thread.sleep(8000);
            }
        }
        
    }
    
}

/*
    We will create an internal private class which extends nanohttpd and 
    helps starts a custom server which prints name of files in server_dir and
    on clicking on these files lets us download these files.
    */
class WebServer extends NanoHTTPD 
{

    public static final int PORT = 12345;

    private HashMap<String, Long> list_of_files;  /* to store filenames and size*/

    private File serverfile;  /* to retreive files in server_dir */

    /* to instantiate a new File object for requested file */
    private File requestedfile; 

    private FileInputStream fis; /* to send file as HTTP response */

        /* the webserver will start on this port */
    public WebServer() {
        super(PORT);
    }

    /* We now override this method to give custom behaviour to the server */
    @Override
    public Response serve(String uri, Method method, Map<String, String> header, 
        Map<String, String> params,
        Map<String, String> files) {

        list_of_files = new HashMap<>(); 

        try {
                /* we get the local_dir */
            serverfile = new File(FileTool.LOCAL_DIR);
            String name;
            Long size;
            for(File file: serverfile.listFiles()) { 

                name = file.getName();
                size = file.length();
             
              /* map all the files and their sizes in the server_dir*/
                list_of_files.put(name, size); 

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* 
        set the behaviour of server when root URL of server is requested 
        */
        if(uri.equals("/")) 
        {  

            /* prepare the html body to display when heading to server */
            //String body = "<html><head><title>FileApp</title></head><body>";
            /* 
            prepare the plain-text body to display when heading to server.
            We are using plain text to make it easily parsable by machine.
            */
            String body = "";


            /* if size of list of zero then we have no files inside server_dir*/
            if(list_of_files.size() != 0) {
                /* 
                if files are present iterate through the filelist and insert their name 
                in the html along with a hyperlink to download them 
                */
                Iterator it = list_of_files.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry element = (Map.Entry) it.next();
                    /*
                    we will display plain text
                    */
                    body = body + element.getKey() + "\t" + element.getValue() + "\n";
                }
                    
            }

            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, body);
        }

            /*
            now set the behaviour of server when /getfile URL of server is requested 
            */
        else if(uri.equals("/getfile")) 
        {
            requestedfile = null;
            fis = null;

            try {
                /*instantiate a File object for the requested file*/
                requestedfile = new File(FileTool.LOCAL_DIR + "/" + 
                    URLDecoder.decode(params.get("f_n")));
                    
                /* and open the File input stream for that file */
                fis = new FileInputStream(requestedfile);

                /* prepare to get the MimeType of the filename */
                MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
                String mime_type = mimeTypes.getContentType(requestedfile);
                return newChunkedResponse(Response.Status.OK, mime_type, fis);

            } catch (Exception e) {
                e.printStackTrace();

                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                    MIME_PLAINTEXT, "Internal Server Error");
            }
        }

            /*
            if any other URL is requested, show 404 error
            */
        else 
        {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, 
                MIME_PLAINTEXT, "404 REQUESTED PAGE NOT FOUND");
        }
    }
} 


