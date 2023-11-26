# DistributedSystems

[![Windows](https://img.shields.io/badge/Windows-11-blue?style=flat-square&logo=windows&logoColor=white)](https://www.microsoft.com/windows/) [![Eclipse IDE](https://img.shields.io/badge/Eclipse%20IDE-2021--09-5B69E8?style=flat-square&logo=eclipse-ide&logoColor=white)](https://www.eclipse.org/ide/) [![Java](https://img.shields.io/badge/Java-11-ED8B00?style=flat-square&logo=java&logoColor=white)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

Some of Distributed Systems' course assignments.

## Assignment 1
#### Package rmi.clientserver.assignment1
This is client - server application developed with RMI for the purchase of products. 

This application involves a server and a certain number of clients (at least 3).
Products have a random price between 10 and 200, generated periodically by the server. The server informs all the clients about the value.
Each client receives the prices of the product and randomly generates the maximum purchase price (always in the range between 10 and 200 ).
If the sale price is lower than the price maximum purchase he sends a purchase request to the server.
Once the purchase request has been received, the server sends a sale confirmation if the purchase price is greater than or equal to the current sale price; otherwise send a rejection message.
Each client terminates its activity after the same number of purchases (at least 10). When all clients have completed their purchases, the server terminates the application.


## Assignment 2
#### Package socket.clientserver.assignment2
Implementation of assignment 1 using socket.
