import java.io.*;
import java.net.*;
import java.util.*;

public class MyWebServer
{
    // Port number the server will listen on
    private static int port = 8888;

    // Root directory where files will be served from
    private static String rootDirectory = ".";

    public static void main(String[] args) throws IOException
    {
        // Create a server socket (this opens the "door" for clients)
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Server running on port " + port);

        // Infinite loop so server keeps running
        while (true)
        {
            // Wait for a client to connect
            Socket clientSocket = serverSocket.accept();

            // Create a new thread for EACH client (handles concurrency requirement)
            Thread t = new Thread(new ClientHandler(clientSocket));
            t.start();
        }
    }

    // This class handles each client connection separately
    private static class ClientHandler implements Runnable
    {
        private Socket socket;

        public ClientHandler(Socket socket)
        {
            this.socket = socket;
        }

        public void run()
        {
            try
            {
                // Input stream (client -> server)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                // Output stream (server -> client)
                OutputStream out = socket.getOutputStream();

                // Used to support persistent connections
                boolean keepAlive = true;

                // Loop to allow multiple requests on SAME connection
                while (keepAlive)
                {
                    // Read the first line of HTTP request (ex: GET /index.html HTTP/1.1)
                    String requestLine = in.readLine();

                    // If client disconnects, stop loop
                    if (requestLine == null) break;

                    // Print request to console (required by assignment)
                    System.out.println("REQUEST: " + requestLine);

                    // Break request into parts
                    StringTokenizer tokenizer = new StringTokenizer(requestLine);

                    String method = tokenizer.nextToken();   // GET / HEAD / POST
                    String fileName = tokenizer.nextToken(); // /index.html

                    // If user just typed "/", serve index.html by default
                    if (fileName.equals("/"))
                        fileName = "/index.html";

                    // Build full file path
                    fileName = rootDirectory + fileName;

                    // Read remaining headers
                    String line;
                    while (!(line = in.readLine()).equals(""))
                    {
                        // If client wants to close connection
                        if (line.toLowerCase().contains("connection: close"))
                        {
                            keepAlive = false;
                        }
                    }

                    // SECURITY: block directory traversal attacks (../)
                    if (fileName.contains(".."))
                    {
                        sendResponse(out, "403 Forbidden", "", 0);
                        continue;
                    }

                    File file = new File(fileName);

                    // If file doesn't exist → 404
                    if (!file.exists())
                    {
                        sendResponse(out, "404 Not Found", "", 0);
                        continue;
                    }

                    // If method is not GET or HEAD → 501
                    if (!method.equals("GET") && !method.equals("HEAD"))
                    {
                        sendResponse(out, "501 Not Implemented", "", 0);
                        continue;
                    }

                    // Read file into byte array
                    byte[] fileData = readFile(file);

                    // Send HTTP headers
                    sendHeaders(out, "200 OK", fileData.length, file.lastModified());

                    // If GET → send file content
                    if (method.equals("GET"))
                    {
                        out.write(fileData);
                    }

                    // Send everything immediately
                    out.flush();
                }

                // Close connection when done
                socket.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // Sends HTTP headers
        private void sendHeaders(OutputStream out, String status, int length, long lastModified) throws IOException
        {
            PrintWriter writer = new PrintWriter(out);

            writer.println("HTTP/1.1 " + status);               // Status line
            writer.println("Date: " + new Date());              // Current date
            writer.println("Server: MyJavaServer");             // Server name
            writer.println("Content-Length: " + length);        // Size of response
            writer.println("Last-Modified: " + new Date(lastModified)); // File last modified
            writer.println("Connection: keep-alive");           // Keep connection open
            writer.println();                                   // Blank line REQUIRED

            writer.flush();
        }

        // Sends simple responses (used for errors like 404, 403, 501)
        private void sendResponse(OutputStream out, String status, String body, int length) throws IOException
        {
            PrintWriter writer = new PrintWriter(out);

            writer.println("HTTP/1.1 " + status);
            writer.println("Date: " + new Date());
            writer.println("Server: MyJavaServer");
            writer.println("Content-Length: " + length);
            writer.println();   // blank line

            writer.println(body);

            writer.flush();
        }

        // Reads file into byte array so it can be sent over network
        private byte[] readFile(File file) throws IOException
        {
            FileInputStream fis = new FileInputStream(file);

            byte[] data = new byte[(int) file.length()];

            fis.read(data);
            fis.close();

            return data;
        }
    }
}