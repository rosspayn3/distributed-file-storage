# Distributed Systems Final Project Fall '20

## Introduction

Our team developed a distributed database system based on the requirements given in the final project document. The system allows users to register and sign in with a username and
password via a web application hosted at http://dist.cs.uafs.edu:32121/dist10. The Apache Tomcat web application and main database server run on the public-facing server. File server nodes run on one or more of the internal servers. Files uploaded from the browser are forwarded from the main server to one or more of the available file servers, then written to disk in the user’s directory.

## Features

- Web application is functional for desktop and mobile devices
- Simple register/login page
- Page for uploading files
- Page for viewing/removing files
- Redirect if logged in or not depending on the page
- Web application uses the browser session to remember the current user until the session is invalidated
- Logout button to invalidate browser session
- Arbitrary file size limit to maintain reasonable performance
- User-friendly web app GUI to assist in viewing and removing uploaded files
- Metadata concerning usernames, files associated with a username, and which data nodes contain a particular file is stored on the main server.
- A directory is created for each user to keep files sorted
- Automatic load balancing when adding new files to data nodes
- Redistribution of files if a data node explodes

## Technical Specifications

- When a client connects, a new thread is started that handles input and responses to that client
- Extensive use of hash maps to improve data access speed
- Implemented reentrant locks to control concurrent access to servers during file operations
- Transferring files occurs by reading bytes from an input stream and immediately writing the file data to an output stream
