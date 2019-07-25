#pragma once

/*
* AES/ECB/PKCS5Padding
*/

#ifdef __cplusplus
extern "C" {
#endif

#define AES_ENCRYPT     1
#define AES_DECRYPT     0

	typedef struct _aes_context
	{
		int nr;                     /*!<  number of rounds  */
		unsigned int* rk;               /*!<  AES round keys    */
		unsigned int buf[68];           /*!<  unaligned data    */
	}aes_context;


	void aes_setkey_enc(aes_context* ctx, const unsigned char* key, int keysize);
	void aes_setkey_dec(aes_context* ctx, const unsigned char* key, int keysize);
	void aes_crypt_ecb_update(aes_context* ctx, int mode, const unsigned char input[16], unsigned char output[16]);
	unsigned char* aes_crypt_ecb(aes_context* ctx, int mode, const unsigned char* input, int slen, int* dlen);

#ifdef __cplusplus
}
#endif