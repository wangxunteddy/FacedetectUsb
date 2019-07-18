#include <WinSock2.h>  
#include <stdio.h>
#include <cpprest/uri.h>
#include <cpprest/http_listener.h>
#include <cpprest/asyncrt_utils.h>

#pragma comment(lib,"ws2_32.lib")

using namespace web;
using namespace http;
using namespace utility;
using namespace http::experimental::listener;
//using namespace std;

#define WRITE_T_LOG_ENABLE 0

#define RECEIVE_PORT	55530
#define SLEEP_TIME 5000 //间隔时间
#define FILE_PATH "C:\\IPReportSrvLog.txt" //信息输出文件

bool brun = false;

SERVICE_STATUS servicestatus;
SERVICE_STATUS_HANDLE hstatus;
class CommandHandler;
CommandHandler* pCmdHandler = 0;
std::map<std::string, std::string> IPMap;
//std::string IPAddress = "";

int WriteToLog(const char* str);

void IPReceiveWork();
void IPReportWork();

void WINAPI ServiceMain(int argc, char** argv);

void WINAPI CtrlHandler(DWORD request);

class CommandHandler
{
public:
	CommandHandler() {}
	~CommandHandler() { m_listener.close(); }
	CommandHandler(utility::string_t url);
	pplx::task<void> open() { return m_listener.open(); }
	pplx::task<void> close() { return m_listener.close(); }
private:
	void handle_get_or_post(http_request message);
	http_listener m_listener;
};

CommandHandler::CommandHandler(utility::string_t url) : m_listener(url)
{
//	m_listener.support(methods::GET, std::bind(&CommandHandler::handle_get_or_post, this, std::placeholders::_1));
	m_listener.support(methods::POST, std::bind(&CommandHandler::handle_get_or_post, this, std::placeholders::_1));
}

void CommandHandler::handle_get_or_post(http_request message)
{
//	ucout << "Method: " << message.method() << std::endl;
//	ucout << "URI: " << http::uri::decode(message.relative_uri().path()) << std::endl;
//	ucout << "Query: " << http::uri::decode(message.relative_uri().query()) << std::endl << std::endl;

	std::string ProductSn;
	ProductSn.clear();
	const json::value& jval = message.extract_json().get();
	const web::json::object& jobj = jval.as_object();
	if (jval.has_field(U("ProductSn"))) {
		utility::string_t str = jobj.at(L"ProductSn").as_string();
		ProductSn = utility::conversions::to_utf8string(str);
	}

	std::string replymsg = "";
	if (ProductSn.size() == 0 && IPMap.size() == 1) {
		// 未指定sn且仅有一台IP记录
		replymsg = IPMap.begin()->second;
	}
	else if(ProductSn.size() > 0 && IPMap.size() > 0) {
		auto iter = IPMap.find(ProductSn);
		if (iter != IPMap.end())
			replymsg = iter->second;
	}
	message.reply(status_codes::OK, replymsg);
};

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

void analyseReceiveMsg(char* msg)
{
	std::string msgstr = msg;
	std::string ProductSn, IPAddress;
	ProductSn.clear();
	IPAddress.clear();
	std::vector<std::string> s1,s2;
	s1 = split(msgstr, ';');
	if (s1.size() == 2) {
		s2 = split(s1[0], ':');
		if (s2.size() == 2 && strcmp(s2[0].c_str(),"ProductSn") == 0 && strcmp(s2[1].c_str(),"null") != 0) {
			ProductSn = s2[1];
		}
		s2 = split(s1[1], ':');
		if (s2.size() == 2 && strcmp(s2[0].c_str(),"IPAddress") == 0) {
			IPAddress = s2[1];
		}
	}

	if (ProductSn.size() > 0 && IPAddress.size() > 0) {
		auto iter = IPMap.find(ProductSn);
		if (iter != IPMap.end()) {
			iter->second = IPAddress;
		}
		else
			IPMap.insert(std::pair<std::string, std::string>(ProductSn, IPAddress));
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
	IPMap.clear();
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
		analyseReceiveMsg(buff);
		WriteToLog(buff);
		//printf("%s\n", IPAddress.c_str());
	}
}

void IPReportWork()
{
	try
	{
		utility::string_t address = U("http://localhost:55531");
		uri_builder uri(address);
		auto addr = uri.to_uri().to_string();
		pCmdHandler = new CommandHandler(addr);
		pCmdHandler->open().wait();
	}
	catch (std::exception& ex)
	{
		if (pCmdHandler) {
			delete pCmdHandler;
			pCmdHandler = 0;
		}
		std::string exstr = "Exception: ";
		exstr+=ex.what();
		WriteToLog(exstr.c_str());
	}

	
	return;
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
//	IPReportWork();
//	IPReceiveWork();
}
