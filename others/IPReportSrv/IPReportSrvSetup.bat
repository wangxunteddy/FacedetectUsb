@echo off

sc create IPReportSrv binpath=%~dp0IPReportSrv.exe
sc config IPReportSrv start=AUTO
sc start IPReportSrv

netsh advfirewall firewall delete rule name=UDPPort55530 dir=in protocol=UDP localport=55530
netsh advfirewall firewall add rule name=UDPPort55530 dir=in action=allow protocol=UDP localport=55530

pause