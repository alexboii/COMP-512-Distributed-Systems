# Running Instructions

*Make sure to have `sshpass` installed and a McGill CS account prior to following these steps!*

To compile all of the servers, distribute them into the respective machines (which you can choose by editing the ServerConstants file 
in /Servers/Common/Constants/ServerConstants and in the script ./run_servers.sh), and run them, simply execute: 

```
sudo ./run_servers.sh <Your McGill CS Username> <Your McGill CS Password> 
```

To run the RMI client:

```
sudo ./run_client.sh
```

I suggest you run the client script from a McGill machine. 