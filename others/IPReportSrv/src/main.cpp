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

#define WRITE_T_LOG_ENABLE 0

#define RECEIVE_PORT	55530
#define SLEEP_TIME 5000 //���ʱ��
#define FILE_PATH "C:\\IPReportSrvLog.txt" //��Ϣ����ļ�

std::string IPAddress = "";

bool brun = false;

SERVICE_STATUS servicestatus;
SERVICE_STATUS_HANDLE hstatus;
class CommandHandler;
CommandHandler* pCmdHandler = 0;

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
	m_listener.support(methods::GET, std::bind(&CommandHandler::handle_get_or_post, this, std::placeholders::_1));
//	m_listener.support(methods::POST, std::bind(&CommandHandler::handle_get_or_post, this, std::placeholders::_1));
}

void CommandHandler::handle_get_or_post(http_request message)
{
//	ucout << "Method: " << message.method() << std::endl;
//	ucout << "URI: " << http::uri::decode(message.relative_uri().path()) << std::endl;
//	ucout << "Query: " << http::uri::decode(message.relative_uri().query()) << std::endl << std::endl;
	message.reply(status_codes::OK, IPAddress);
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
	servicestatus.dwControlsAccepted = SERVICE_ACCEPT_SHUTDOWN | SERVICE_ACCEPT_STOP;//�ڱ�����ֻ����ϵͳ�ػ���ֹͣ�������ֿ�������
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

	//��SCM ��������״̬
	servicestatus.dwCurrentState = SERVICE_RUNNING;
	SetServiceStatus(hstatus, &servicestatus);

	//�ڴ˴�������Լ�ϣ���������Ĺ����������������Ĺ����ǻ�õ�ǰ���õ�����������ڴ���Ϣ
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

void IPReceiveWork()
{
	WSADATA wsaData;
	int err;

	// ����socket api 
	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
		return;
	}

	// ����socket  
	SOCKET connect_socket;
	connect_socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	if (INVALID_SOCKET == connect_socket) {
		WriteToLog("socket error!");
		return;
	}

	// �������׽���  
	SOCKADDR_IN sin;
	sin.sin_family = AF_INET;
	sin.sin_port = htons(RECEIVE_PORT);             //���Ͷ�ʹ�õķ��Ͷ˿�
	sin.sin_addr.s_addr = 0;

	// �����������ϵĹ㲥��ַ��������
	SOCKADDR_IN sin_from;
	sin_from.sin_family = AF_INET;
	sin_from.sin_port = htons(RECEIVE_PORT);
	sin_from.sin_addr.s_addr = INADDR_BROADCAST;

	//���ø��׽���Ϊ�㲥���ͣ�  
	bool bOpt = true;
	setsockopt(connect_socket, SOL_SOCKET, SO_BROADCAST, (char*)&bOpt, sizeof(bOpt));

	// ���׽���
	err = bind(connect_socket, (SOCKADDR*)&sin, sizeof(SOCKADDR));
	if (SOCKET_ERROR == err) {
		err = WSAGetLastError();
		WriteToLog("bind error!");
		return;
	}

	int nAddrLen = sizeof(SOCKADDR);
	char buff[512] = "";       //������ջ�����
	brun = true;
	WriteToLog("init OK!");
	while (brun) {
		// ��������  
		int nSendSize = recvfrom(connect_socket, buff, 512, 0, (SOCKADDR*)&sin_from, &nAddrLen);
		if (SOCKET_ERROR == nSendSize) {
			//����ʧ��
			err = WSAGetLastError();
			WriteToLog("recvfrom error!");
			continue;
		}

		buff[nSendSize] = '\0';   //�ַ�����ֹ
		IPAddress = buff;
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
