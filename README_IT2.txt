===========================================================================================================
# SYSC3303 : Project - Iteration #2
-----------------------------------------------------------------------------------------------------------
# 16/05/15
# Group #1:
  Ben Croskery      (100973306)
  Ben Earle         (100970237)
  Dillon Verhaeghe  (100965889)
  Patrick Perron    (100965104)
  Shane Corrigan    (100965710)
  
===========================================================================================================
# Responsibilities
-----------------------------------------------------------------------------------------------------------
  Ben Croskery     -> unit testing, terminate incompleted files in bad transfers, testing of packet checking
  Ben Earle        -> UML Sequence diagrams, UDP helper class and replacing all old udp code, client code
  Dillon Verhaeghe -> Created packet checking methods for various packet types, fixed bugs
  Patrick Perron   -> Error Sim interface, sabotaging packets with Error Sim, Debugging
  Shane Corrigan   -> Updated client to use UDPHelper, UML class, client debugging 
  
===========================================================================================================
# Files
-----------------------------------------------------------------------------------------------------------
 Main Classes:
  * Server.java --------------- Code for Server User Interface, launches ControlThread
  * Client.java --------------- Code for Client that will send Read/Write requests
  * ErrorSimulator.java ------- Code for Error simulator, that forward messages back and forth between 
                                client and server. Sabotages packets to test errors if desired
  * ReadThread.java ----------- Server Thread for handling read request from server
  * WriteThread.java ---------- Server Thread for handling write request from server
  * ControlThread.java -------- Server Thread for listening for requests from clients and creating 
                                threads to handle them
  * ClientResponseThread.java - Abstract class with shared code for read/write threads
 ----------------------------------------------------------------------------------------------------------
 Util Classes:
  * util/ErrorScenario.java --- Describes an error case to test for ErrorSimulator
  * util/Var.java ------------- Contains shared Constants
  * util/Log.java ------------- Contains methods for controlling logging to console
  * util/FileReader.java ------ Class to handle writing bytes to a file
  * util/FileWriter.java ------ Class to handle readinh bytes from a file
  * util/TFTPErrorHelper.java - Static class that checks packets for specific errors
  * util/UDPHelper.java ------- Class to faciliate UDP send and receive operations
 ----------------------------------------------------------------------------------------------------------
 Diagrams:

***CHANGE***
  * Diagrams/Iteration1/IT1 RRQ Connection.png------ Use Case Map for read requests connection cycle
  * Diagrams/Iteration1/IT1 WRQ Connection.png------ Use Case Map for write requests connection cycle
  * Diagrams/Iteration1/IT1 RRQ Data Transfer.png--- Use Case Map for read requests data transfer cycle
  * Diagrams/Iteration1/IT1 WRQ Data Transfer.png--- Use Case Map for write requests data transfer cycle
  * Diagrams/Iteration1/UML_default.PNG ------------ UML Class diagram for main classes
  * Diagrams/Iteration1/UML_util.PNG --------------- UML Class diagram for util classes
*****

 ----------------------------------------------------------------------------------------------------------
 Test Files:
  * c_0.txt, s_0.txt ----------- Empty file
  * c_512.txt, s_512.txt ------- File with 512 bytes of ASCII characters
  * c_1221.txt, s_1221.txt ----- File with 1221 bytes of ASCII characters
  * c_bee.png, s_bee.png ------- Picture of bee in png format
  * c_bee.txt, s_bee.txt ------- Script to "Bee Movie" stored as ASCII characters
  * c_jpg.jpg, s_jpg.jpg ------- Basic jpg file stored as binary
  * 50mb.zip-------------------- Large file to test wrap around of block numbers
  
 **NOTE: s_* and c_* convention for test files is used to indicate if file originally existed in 
 **      client root folder or server root folder. Client files in /src/testFile/, Server files 
 **      in /src/testFile/server/. 
===========================================================================================================
# Instructions for running
-----------------------------------------------------------------------------------------------------------
 Setup:
      1) Compile and run Server.java
          - For VERBOSE mode: enter 'v' or 'verbose' to toggle verbose after startup, OR pass 'v' as
            an initial argument
      2) (OPTIONAL) Compile and run ErrorSimulator.java
	  - Select desired error(4,5) or no error mode(0).
      3) Compile and run Client.java
          - For VERBOSE mode: enter 'v' or 'verbose' to toggle verbose after startup, OR pass 'v' as
            an initial argument
          - For error simulator mode: enter 't' or 'test' to toggle test mode after startup, OR pass 't' as
            an initial argument
 ----------------------------------------------------------------------------------------------------------
 Example Write Test:
      1) Enter 'W" on client console
      2) Enter a file name found in the client root from Test Files mentioned above.
            -File should be written as c_* to indicate it is in client root (i.e. c_512.txt, c_bee.txt)
      3) In /src/testFile/server/, an identical copy of file should be found
 ----------------------------------------------------------------------------------------------------------
 Example Read Test:
      1) Enter 'R" on client console
      2) Enter a file name found in the server root from Test Files mentioned above.
            -File should be written as s_* to indicate it is in server root (i.e. s_0.txt, s_jpg.jpg)
      3) In /src/testFile/, an identical copy of file should be found 
 ----------------------------------------------------------------------------------------------------------
 Example Error Test:
      1) Run Error Simulator
      2) Pick desired error scenario specification using command line interface
      3) Run a read or write request to trigger specified error case
 ----------------------------------------------------------------------------------------------------------
 Quitting:
      1) Enter 'S' on client console
      2) Type 'S' on Server console
      3) (OPTIONAL)Ctrl+C on Error Simulator console
      
===========================================================================================================
# Github source link
-----------------------------------------------------------------------------------------------------------
      https://github.com/BenEarle/SYSC3303-Project
      
===========================================================================================================
  HAVE FUN!!!!
===========================================================================================================
