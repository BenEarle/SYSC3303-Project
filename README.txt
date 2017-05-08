===========================================================================================================
# SYSC3303 : Project - Iteration #1
-----------------------------------------------------------------------------------------------------------
# 09/05/15
# Group #1:
  Ben Croskery (100973306)
  Ben Earle (100970237)
  Dillon Verhaeghe (10096889)
  Patrick Perron (100965104)
  Shane Corrigan (100965710)
  
===========================================================================================================
# Responsibilities
-----------------------------------------------------------------------------------------------------------
  Ben Croskery     -> Provided base code, util classes, FileRead/Writer Classes, Test Classes, Client Read/Write
  Ben Earle        -> Server main, control, read and write threads, formatting, and comments  
  Dillon Verhaeghe -> Client side support, verbose mode, test mode, conforming to TFTP protocal
  Patrick Perron   -> Writing threads for server, debugging read/write transfers, Main ErrorSimulator code
  Shane Corrigan   -> UMC Diagrams, Server main user interface  
  
=====================================================================================================================
# Files
---------------------------------------------------------------------------------------------------------------------
 Main Classes:
  * Server.java --------------- Code for Server User Interface, launches ControlThread
  * Client.java --------------- Code for Client that will send Read/Write requests
  * ErrorSimulator.java ------- Code for Error simulator, thht forward messages back and forth between client and server
  * ReadThread.java ----------- Server Thread for handling read request from server
  * WriteThread.java ---------- Server Thread for handling write request from server
  * ControlThread.java -------- Server Thread for listening for requests from clients and creating threads to handle them
  * ClientResponseThread.java - Abstract class with shared code for read/write threads
 --------------------------------------------------------------------------------------------------------------------
 Util Classes:
  * util/Var.java ------------- Contains shared Constants
  * util/Log.java ------------- Contains methods for controlling logging to console
  * util/FileReader.java ------ Class to handle writing bytes to a file
  * util/FileWriter.java ------ Class to handle readinh bytes from a file
 --------------------------------------------------------------------------------------------------------------------
 Diagrams:
  * Diagrams/Iteration1/Read Error Sim.xml ---- Use Case Map for Read request using ErrorSimulator
  * Diagrams/Iteration1/Write Error Sim.xml --- Use Case Map for Write request using ErrorSimulator
  * Diagrams/Iteration1/UML_default.PNG ------- UML Class diagram for main classes
  * Diagrams/Iteration1/UML_util.PNG ---------- UML Class diagram for util classes
 --------------------------------------------------------------------------------------------------------------------
 Test Files:
  * c_0.txt, s_0.txt ----------- Empty file
  * c_512.txt, s_512.txt ------- File with 512 bytes of ASCII characters
  * c_1221.txt, s_1221.txt ----- File with 1221 bytes of ASCII characters
  * c_bee.png, s_bee.png ------- Script to "Bee Movie" stored as binary
  * c_bee.txt, s_bee.txt ------- Script to "Bee Movie" stored as ASCII characters
  * c_jpg.jpg, s_jpg.jpg ------- Basic jpg file stored as binary
  
  **NOTE: s_* and c_* convention for test files is used to indicate if file originally existed in client root **
  **      folder or server root folder. Client files in /src/testFile/, Server files in /src/testFile/server/. 
=====================================================================================================================
# Instructions for running
---------------------------------------------------------------------------------------------------------------------
 Setup:
      1) Compile and run Server.java
          - For VERBOSE mode: enter 'verbose' after startup, OR pass 'v' as an initial argument
      2) (OPTIONAL) Compile and run ErrorSimulator.java
      3) Compile and run Server.java
          - For VERBOSE mode: enter 'verbose' after startup, OR pass'v' as an inital argument
          - IF using ErrorSimulator: pass 't' as an additional argument (in addition to 'v' if verbose)
 --------------------------------------------------------------------------------------------------------------------
 Example Write Test:
      1) Enter 'W" on client console
      2) Enter a file name found in the client root from Test Files mentioned above.
            -File shoule be written as c_* to indicate it is in client root (i.e. c_512.txt, c_bee.txt)
      3) In /src/testFile/server/, an identical copy of file should be found 
 --------------------------------------------------------------------------------------------------------------------
 Example Read Test:
      1) Enter 'R" on client console
      2) Enter a file name found in the server root from Test Files mentioned above.
            -File shoule be written as s_* to indicate it is in server root (i.e. s_0.txt, s_jpg.jpg)
      3) In /src/testFile/, an identical copy of file should be found
 --------------------------------------------------------------------------------------------------------------------
 Quitting:
      1) Enter 'S' on client console
      2) Type 'quit' on Server console
      3) (OPTIONAL)Ctrl+C on Error Simulator console
      
=====================================================================================================================
HAVE FUN!!!!
=====================================================================================================================
