#include "aes.h"
#include "AESEncryptor.h"
//#include "log.h"

#include <fstream>
using namespace std;

AesEncryptor::AesEncryptor(unsigned char* key)
{
	m_pAesEnc = new aes_context();
	m_pAesDec = new aes_context();
	aes_setkey_enc(m_pAesEnc, key, strlen((const char*)key)<<3);
	aes_setkey_dec(m_pAesDec, key, strlen((const char*)key)<<3);
}


AesEncryptor::~AesEncryptor(void)
{
	delete m_pAesEnc;
	delete m_pAesDec;
}

void AesEncryptor::Byte2Hex(const unsigned char* src, int len, char* dest) {
	for (int i = 0; i<len; ++i) {
		sprintf_s(dest + i * 2, 3, "%02X", src[i]);
	}
}

void AesEncryptor::Hex2Byte(const char* src, int len, unsigned char* dest) {
	int length = len / 2;
	for (int i = 0; i<length; ++i) {
		dest[i] = Char2Int(src[i * 2]) * 16 + Char2Int(src[i * 2 + 1]);
	}
}

int AesEncryptor::Char2Int(char c) {
	if ('0' <= c && c <= '9') {
		return (c - '0');
	}
	else if ('a' <= c && c <= 'f') {
		return (c - 'a' + 10);
	}
	else if ('A' <= c && c <= 'F') {
		return (c - 'A' + 10);
	}
	return -1;
}

string AesEncryptor::EncryptString(string strInfor)
{
	int dlen = 0;
	unsigned char* pOut = aes_crypt_ecb(m_pAesEnc, AES_ENCRYPT,
		(const unsigned char*)strInfor.c_str(), strInfor.length(), &dlen);
	
	char* pHex = new char[2 * dlen + 1];	// +1:预留'\0'
	memset(pHex, '\0', 2 * dlen+1);
	Byte2Hex(pOut, dlen, pHex);
	string retValue = pHex;

	free(pOut);
	delete pHex;

	return retValue;
}

string AesEncryptor::DecryptString(string strMessage) {
	int nLength = strMessage.length() / 2;
	unsigned char* pByte = new unsigned char[nLength];
	memset(pByte, '\0', nLength);
	Hex2Byte(strMessage.c_str(), strMessage.length(), pByte);

	int dlen = 0;
	unsigned char* pOut = aes_crypt_ecb(m_pAesDec, AES_DECRYPT,
		pByte, nLength, &dlen);
	pOut[dlen] = '\0';	// 去掉补位
	string retValue = (char*)pOut;

	free(pOut);
	delete[] pByte;

	return retValue;
}

unsigned char* AesEncryptor::EncryptByte(char* strInfor, int strLen)
{
	int dlen = 0;
	unsigned char* pOut = aes_crypt_ecb(m_pAesEnc, AES_ENCRYPT,
		(const unsigned char*)strInfor, strLen, &dlen);

	return pOut;
}

unsigned char* AesEncryptor::DecryptByte(char* strInfor, int strLen) {
	int dlen = 0;
	unsigned char* pOut = aes_crypt_ecb(m_pAesDec, AES_DECRYPT,
		(const unsigned char*)strInfor, strLen, &dlen);
	pOut[dlen] = '\0';	// 去掉补位

	return pOut;
}

void AesEncryptor::EncryptTxtFile(const char* inputFileName, const char* outputFileName) {
	ifstream ifs;

	// Open file:
	ifs.open(inputFileName);
	if (!ifs) {
		//UNILOGW("AesEncryptor::EncryptTxtFile() - Open input file failed!");
		return;
	}

	// Read config data:
	string strInfor;
	string strLine;
	while (!ifs.eof()) {
		char temp[1024];
		memset(temp, '\0', 1024);
		ifs.read(temp, 1000);
		strInfor += temp;
	}
	ifs.close();

	// Encrypt
	strLine = EncryptString(strInfor);

	// Writefile 
	ofstream ofs;
	ofs.open(outputFileName);
	if (!ofs) {
		//UNILOGW("AesEncryptor::EncryptTxtFile() - Open output file failed!");
		return;
	}
	ofs << strLine;
	ofs.close();
}

void AesEncryptor::DecryptTxtFile(const char* inputFile, const char* outputFile) {
	ifstream ifs;

	// Open file:
	ifs.open(inputFile);
	if (!ifs) {
		//UNILOGW("AesEncryptor::DecryptTxtFile() - Open input file failed!");
		return;
	}

	// Read config data:
	string strInfor;
	string strLine;
	while (!ifs.eof()) {
		char temp[1024];
		memset(temp, '\0', 1024);
		ifs.read(temp, 1000);
		strInfor += temp;
	}
	ifs.close();

	// Encrypt
	strLine = DecryptString(strInfor);

	// Writefile 
	ofstream ofs;
	ofs.open(outputFile);
	if (!ofs) {
		//UNILOGW("AesEncryptor::DecryptTxtFile() - Open output file failed!");
		return;
	}
	ofs << strLine;
	ofs.close();
}