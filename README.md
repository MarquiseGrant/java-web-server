# Java HTTP Web Server

This project is a multithreaded web server built in Java using TCP socket programming.

## Features
- Handles HTTP GET and HEAD requests
- Supports persistent connections (keep-alive)
- Returns proper HTTP responses and headers
- Error handling for 404, 403, and 501
- Concurrent client handling using threads

## How to Run
1. Compile:
   javac MyWebServer.java

2. Run:
   java MyWebServer

3. Open browser:
   http://localhost:8888/index.html
