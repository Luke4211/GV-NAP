# GV-NAP
A Napster Style P2P FTP system. Contains a ClientGUI and HostServer, which should be run from the same directory. server/Server is a centralized server which aids in P2P communication between remote hosts.

# Instructions:

The server hostname should be set to the IP of the centralized server. 

Username can be anything, is used to differentiate between files of the same name owned by multiple clients.

Port is the port number the centralized server is listening on.

Hostname should be set to the IP where the Client and HostServer are located. This allows other clients to access the HostServer to request files.

Keyword can be set to any string to search the filepool for..

To retrieve a file from a remote host, type retr <filename>/<owner's username> (minus the brackets). It should appear directly as it looks in the search field. 
