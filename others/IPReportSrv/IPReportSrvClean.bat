@echo off

sc stop IPreportSrv
sc delete IPReportSrv

netsh advfirewall firewall delete rule name=UDPPort55530 dir=in protocol=UDP localport=55530
