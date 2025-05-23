# Multi-Threaded_File_Downloader

##Primary Class: MultiThreadCopier
Constructor Parameters:
SourceProvider: Interface for accessing the remote file
String outputPath: Destination file path
int workerCount: Number of download threads

##SourceProvider Interface:
long size(): Returns total file size in bytes
SourceReader connect(long offset): Establishes connection starting at specified byte position
SourceReader Interface:
byte read(): Reads and returns the next byte sequentially


Note : This code was HW5 of my advanced programming course (AP) in Sharif University.
