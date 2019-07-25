#include <WinSock2.h>  
#include <stdio.h>
#include <cpprest/uri.h>
#include <cpprest/http_listener.h>
#include <cpprest/asyncrt_utils.h>
#include <cpprest/http_client.h>
#include <mutex> 

#include "AESEncryptor.h"

#pragma comment(lib,"ws2_32.lib")

using namespace web;
using namespace http;
using namespace utility;
using namespace web::http::client;
using namespace http::experimental::listener;
//using namespace std;

#define WRITE_T_LOG_ENABLE 0

#define RECEIVE_PORT	55530
#define SLEEP_TIME 5000 //间隔时间
#define FILE_PATH "C:\\IPReportSrvLog.txt" //信息输出文件

bool brun = false;

SERVICE_STATUS servicestatus;
SERVICE_STATUS_HANDLE hstatus;

// listener
http_listener* pBindIDListener = NULL;
http_listener* pGetIPListener = NULL;
http_listener* pGetLastFDVListener = NULL;


std::string AESKEY = "13c16d397ca54dd4af91a9fa4a0d0c22";
AesEncryptor* pAesE = NULL;

std::mutex mtx;
std::map<std::string, std::string> IPMap;
//std::string IPAddress = "";
std::string strModulePath = "";

int WriteToLog(const char* str);
void saveConfig();

void InitWork();
void IPReceiveWork();
void IPReportWork();

void WINAPI ServiceMain(int argc, char** argv);

void WINAPI CtrlHandler(DWORD request);

// 修改绑定ID
void Callback_bindID(http_request message)
{
//	ucout << "Method: " << message.method() << std::endl;
//	ucout << "URI: " << http::uri::decode(message.relative_uri().path()) << std::endl;
//	ucout << "Query: " << http::uri::decode(message.relative_uri().query()) << std::endl << std::endl;

	std::string deviceID;
	deviceID.clear();
	const json::value& jval = message.extract_json().get();
	const web::json::object& jobj = jval.as_object();
	if (jval.has_field(U("FDVDeviceID"))) {
		utility::string_t str = jobj.at(L"FDVDeviceID").as_string();
		deviceID = utility::conversions::to_utf8string(str);
		mtx.lock();
		IPMap.clear();
		IPMap.insert(std::pair<std::string, std::string>(deviceID, "")); // IP初始化为空
		saveConfig();
		mtx.unlock();
		message.reply(status_codes::OK, "Success");
	}
	else {
		message.reply(status_codes::OK, "");
	}
};

// 获取IP
void Callback_getIP(http_request message)
{
	//	ucout << "Method: " << message.method() << std::endl;
	//	ucout << "URI: " << http::uri::decode(message.relative_uri().path()) << std::endl;
	//	ucout << "Query: " << http::uri::decode(message.relative_uri().query()) << std::endl << std::endl;
	
	std::string replymsg = "";
	mtx.lock();
	if (IPMap.size() > 0) {
		replymsg = IPMap.begin()->second;
	}
	mtx.unlock();
	message.reply(status_codes::OK, replymsg);
};

// 获取最后识别信息
void Callback_getLastFDV(http_request message)
{
	//	ucout << "Method: " << message.method() << std::endl;
	//	ucout << "URI: " << http::uri::decode(message.relative_uri().path()) << std::endl;
	//	ucout << "Query: " << http::uri::decode(message.relative_uri().query()) << std::endl << std::endl;

	std::string deviceIP = "";
	mtx.lock();
	if (IPMap.size() > 0) {
		deviceIP = IPMap.begin()->second;
	}
	mtx.unlock();
	if (deviceIP.length() == 0) {
		// error
		json::value respJSON = json::value::object();
		respJSON[U("err_code")] = json::value::string(utility::conversions::to_string_t("-1"));
		respJSON[U("err_msg")] = json::value::string(utility::conversions::to_string_t(U("目标IP地址未知!")));
		
		http_response resp; 
		resp.headers().add(utility::conversions::to_string_t("Access-Control-Allow-Origin"), 
			utility::conversions::to_string_t("*")); //设置跨域
		resp.set_body(respJSON);
		resp.set_status_code(status_codes::NotFound);
		message.reply(resp);
		return;
	}

	std::string url = "http://" + deviceIP + ":8010/retrieveidfvinfo";
	utility::string_t urlstr = utility::conversions::to_string_t(url);
	http::uri uri = http::uri(urlstr);
	http_client client(uri);
	web::http::http_request getRequest;
	getRequest.set_method(methods::GET);
	try {
		Concurrency::task<web::http::http_response> getTask = client.request(getRequest);
		http_response resp = getTask.get();
		message.reply(resp);
	}
	catch (const std::exception &e) {
		(void)e;
		json::value respJSON = json::value::object();
		respJSON[U("err_code")] = json::value::string(utility::conversions::to_string_t("-1"));
		respJSON[U("err_msg")] = json::value::string(utility::conversions::to_string_t(U("未知错误!")));
		
		http_response resp;
		resp.headers().add(utility::conversions::to_string_t("Access-Control-Allow-Origin"),
			utility::conversions::to_string_t("*")); //设置跨域
		resp.set_body(respJSON);
		resp.set_status_code(status_codes::ExpectationFailed);
		message.reply(resp);
	}
};

std::string ExtractFilePath(const std::string& szFile)
{
	if (szFile == "")
		return "";

	size_t idx = szFile.find_last_of("\\:");

	if (-1 == idx)
		return "";
	return std::string(szFile.begin(), szFile.begin() + idx + 1);
}

int WriteToLog(const char* str)
{
#if WRITE_T_LOG_ENABLE
	FILE* pfile;
	fopen_s(&pfile, FILE_PATH, "a+");
	if (pfile == NULL) {
		return -1;
	}

	fprintf_s(pfile, "%s\n", str);

	fclose(pfile);
#endif

	return 0;
}

void WINAPI ServiceMain(int argc, char** argv)
{
	servicestatus.dwServiceType = SERVICE_WIN32;
	servicestatus.dwCurrentState = SERVICE_START_PENDING;
	servicestatus.dwControlsAccepted = SERVICE_ACCEPT_SHUTDOWN | SERVICE_ACCEPT_STOP;//在本例中只接受系统关机和停止服务两种控制命令
	servicestatus.dwWin32ExitCode = 0;
	servicestatus.dwServiceSpecificExitCode = 0;
	servicestatus.dwCheckPoint = 0;
	servicestatus.dwWaitHint = 0;

	hstatus = ::RegisterServiceCtrlHandler("IPReportSrv", CtrlHandler);

	if (hstatus == 0) {
		WriteToLog("RegisterServiceCtrlHandler failed");
		return;
	}

	WriteToLog("RegisterServiceCtrlHandler success");

	//向SCM 报告运行状态
	servicestatus.dwCurrentState = SERVICE_RUNNING;
	SetServiceStatus(hstatus, &servicestatus);

	//在此处添加你自己希望服务做的工作，在这里我做的工作是获得当前可用的物理和虚拟内存信息
	InitWork();
	IPReportWork();
	IPReceiveWork();

	WriteToLog("service stopped");
}

void WINAPI CtrlHandler(DWORD request)
{
	switch (request) {
	case SERVICE_CONTROL_STOP:
		brun = false;
		servicestatus.dwCurrentState = SERVICE_STOPPED;
		break;

	case SERVICE_CONTROL_SHUTDOWN:
		brun = false;
		servicestatus.dwCurrentState = SERVICE_STOPPED;
		break;

	default:
		break;
	}

	SetServiceStatus(hstatus, &servicestatus);
}

std::vector<std::string> split(std::string strtem, char a)
{
	std::vector<std::string> strvec;

	std::string::size_type pos1, pos2;
	pos2 = strtem.find(a);
	pos1 = 0;
	while (std::string::npos != pos2)
	{
		strvec.push_back(strtem.substr(pos1, pos2 - pos1));

		pos1 = pos2 + 1;
		pos2 = strtem.find(a, pos1);
	}
	strvec.push_back(strtem.substr(pos1));
	return strvec;
}

void saveConfig()
{
	// 保存IPMap
	std::ofstream saveFile(strModulePath + "config.txt");
	if (IPMap.size() == 0) {
		saveFile << "FDVDeviceID:" << std::endl;
		saveFile << "IP:" << std::endl;
	}
	else {
		for (auto iter = IPMap.begin(); iter != IPMap.end(); iter++) {
			saveFile << "FDVDeviceID:" << iter->first << std::endl;
			saveFile << "IP:" << pAesE->EncryptString(iter->second) << std::endl;
		}
	}
	saveFile.close();
}

void analyseReceiveMsg(char* msg, int msgLen)
{
	char* decByte = (char*)pAesE->DecryptByte(msg, msgLen);
	std::string msgstr = decByte;
	std::string DeviceID, IPAddress;
	DeviceID.clear();
	IPAddress.clear();
	std::vector<std::string> s1,s2;
	s1 = split(msgstr, ';');
	if (s1.size() == 3) {
		if (strcmp(s1[0].c_str(),"null") != 0) {
			DeviceID = s1[0];
		}
		IPAddress = s1[2];
	}

	if (DeviceID.size() > 0 && IPAddress.size() > 0) {
		mtx.lock();
		auto iter = IPMap.find(DeviceID);
		if (iter != IPMap.end()) {
			if (strcmp(iter->second.c_str(), IPAddress.c_str()) != 0) {
				iter->second = IPAddress;
				saveConfig();
			}
		}
		else {
			// 非绑定设备的信息忽略
		//	IPMap.insert(std::pair<std::string, std::string>(DeviceID, IPAddress));
		}
		mtx.unlock();
	}
}



void InitWork()
{
	char szPath[1024] = { 0 };
	GetModuleFileName(NULL, szPath, MAX_PATH);
	strModulePath = ExtractFilePath(szPath);

	// aes
	pAesE = new AesEncryptor((unsigned char*)AESKEY.c_str());

	// config
	IPMap.clear();
	std::string deviceID = "";
	std::string deviceIP = "";
	std::ifstream saveFile(strModulePath + "config.txt");
	if (!saveFile.is_open()) {
		saveConfig();
	}
	else {
		std::string line;
		while (std::getline(saveFile, line)) {
			std::istringstream is_line(line);
			std::string key;
			if (std::getline(is_line, key, ':')) {
				std::string value;
				if (std::getline(is_line, value)) {
					if (key == "FDVDeviceID") {
						deviceID = value;
						std::transform(deviceID.begin(), deviceID.end(), deviceID.begin(), ::toupper);
					}
					if (key == "IP")
						deviceIP = pAesE->DecryptString(value);
				}
			}
		}
		if (deviceID.length() > 0) {
			IPMap.insert(std::pair<std::string, std::string>(deviceID, deviceIP));
		}
		saveFile.close();
	}
}

void IPReceiveWork()
{
	WSADATA wsaData;
	int err;

	// 启动socket api 
	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
		return;
	}

	// 创建socket  
	SOCKET connect_socket;
	connect_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	if (INVALID_SOCKET == connect_socket) {
		WriteToLog("socket error!");
		return;
	}

	// 用来绑定套接字  
	SOCKADDR_IN sin;
	sin.sin_family = AF_INET;
	sin.sin_port = htons(RECEIVE_PORT);             //发送端使用的发送端口
	sin.sin_addr.s_addr = 0;

	// 用来从网络上的广播地址接收数据
	SOCKADDR_IN sin_from;
	sin_from.sin_family = AF_INET;
	sin_from.sin_port = htons(RECEIVE_PORT);
	sin_from.sin_addr.s_addr = INADDR_BROADCAST;

	//设置该套接字为广播类型，  
	bool bOpt = true;
	setsockopt(connect_socket, SOL_SOCKET, SO_BROADCAST, (char*)&bOpt, sizeof(bOpt));

	// 绑定套接字
	err = bind(connect_socket, (SOCKADDR*)&sin, sizeof(SOCKADDR));
	if (SOCKET_ERROR == err) {
		err = WSAGetLastError();
		WriteToLog("bind error!");
		return;
	}

	int nAddrLen = sizeof(SOCKADDR);
	char buff[512] = "";       //定义接收缓冲区
	brun = true;
	WriteToLog("init OK!");
	while (brun) {
		// 接收数据  
		int nSendSize = recvfrom(connect_socket, buff, 512, 0, (SOCKADDR*)&sin_from, &nAddrLen);
		if (SOCKET_ERROR == nSendSize) {
			//接收失败
			err = WSAGetLastError();
			WriteToLog("recvfrom error!");
			continue;
		}

		buff[nSendSize] = '\0';   //字符串终止
		analyseReceiveMsg(buff, nSendSize);
		WriteToLog(buff);
		//printf("%s\n", IPAddress.c_str());
	}
}

void IPReportWork()
{
	try
	{
		utility::string_t address = U("http://localhost:55531/bindID");
		uri_builder uri(address);
		auto addr = uri.to_uri().to_string();
		pBindIDListener = new http_listener(addr);
		pBindIDListener->support(methods::POST, &Callback_bindID);
		pBindIDListener->open().wait();
	}
	catch (std::exception& ex)
	{
		if (pBindIDListener) {
			pBindIDListener->close().wait();
			delete pBindIDListener;
			pBindIDListener = 0;
		}
		std::string exstr = "Exception: ";
		exstr += ex.what();
		WriteToLog(exstr.c_str());
	}

	try
	{
		utility::string_t address = U("http://localhost:55531/getIP");
		uri_builder uri(address);
		auto addr = uri.to_uri().to_string();
		pGetIPListener = new http_listener(addr);
		pGetIPListener->support(methods::POST, &Callback_getIP);
		pGetIPListener->open().wait();
	}
	catch (std::exception& ex)
	{
		if (pGetIPListener) {
			pGetIPListener->close().wait();
			delete pGetIPListener;
			pGetIPListener = 0;
		}
		std::string exstr = "Exception: ";
		exstr+=ex.what();
		WriteToLog(exstr.c_str());
	}

	try
	{
		utility::string_t address = U("http://localhost:55531/retrieveidfvinfo");
		uri_builder uri(address);
		auto addr = uri.to_uri().to_string();
		pGetLastFDVListener = new http_listener(addr);
		pGetLastFDVListener->support(methods::GET, &Callback_getLastFDV);
		pGetLastFDVListener->open().wait();
	}
	catch (std::exception& ex)
	{
		if (pGetLastFDVListener) {
			pGetLastFDVListener->close().wait();
			delete pGetLastFDVListener;
			pGetLastFDVListener = 0;
		}
		std::string exstr = "Exception: ";
		exstr += ex.what();
		WriteToLog(exstr.c_str());
	}
	return;
}

void releaseWork()
{
	if (pAesE) {
		delete pAesE;
		pAesE = NULL;
	}
	if (pBindIDListener) {
		pBindIDListener->close().wait();
		delete pBindIDListener;
		pBindIDListener = NULL;
	}
	if (pGetIPListener) {
		pGetIPListener->close().wait();
		delete pGetIPListener;
		pGetIPListener = NULL;
	}
	if (pGetLastFDVListener) {
		pGetLastFDVListener->close().wait();
		delete pGetLastFDVListener;
		pGetLastFDVListener = NULL;
	}
}

void Byte2Hex(const unsigned char* src, int len, char* dest) {
	for (int i = 0; i<len; ++i) {
		sprintf_s(dest + i * 2, 3, "%02X", src[i]);
	}
}
void main()
{
//*
	SERVICE_TABLE_ENTRY entrytable[2];
	entrytable[0].lpServiceName = "IPReportSrv";
	entrytable[0].lpServiceProc = (LPSERVICE_MAIN_FUNCTION)ServiceMain;
	entrytable[1].lpServiceName = NULL;
	entrytable[1].lpServiceProc = NULL;
	StartServiceCtrlDispatcher(entrytable);
//*/

//	InitWork();
//	IPReportWork();
//	IPReceiveWork();

	//releaseWork();
}
