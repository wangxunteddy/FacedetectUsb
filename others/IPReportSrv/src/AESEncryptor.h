#pragma once

#include <string>
#include "aes.h"

class AesEncryptor
{
public:
	AesEncryptor(unsigned char* key);
	~AesEncryptor(void);

	std::string EncryptString(std::string strInfor);
	std::string DecryptString(std::string strMessage);

	unsigned char* EncryptByte(char* strInfor, int strLen);
	unsigned char* DecryptByte(char* strInfor, int strLen);

	void EncryptTxtFile(const char* inputFileName, const char* outputFileName);
	void DecryptTxtFile(const char* inputFileName, const char* outputFileName);

private:
	void Byte2Hex(const unsigned char* src, int len, char* dest);
	void Hex2Byte(const char* src, int len, unsigned char* dest);
	int  Char2Int(char c);

private:
	aes_context* m_pAesEnc;
	aes_context* m_pAesDec;
};
