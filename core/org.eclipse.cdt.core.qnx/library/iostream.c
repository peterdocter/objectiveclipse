/* Copyright, 2002, QNX Software Systems Ltd.  All Rights Reserved

 * This source code has been published by QNX Software Systems
 * Ltd. (QSSL). However, any use, reproduction, modification, distribution
 * or transfer of this software, or any software which includes or is based
 * upon any of this code, is only permitted if expressly authorized by a
 * written license agreement from QSSL. Contact the QNX Developer's Network
 * or contact QSSL's legal department for more information.
 *
 *
 *  iostream.c
 *
 *  This is a JNI implementation of access to standard i/o streams 
 */
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/uio.h>
#include <errno.h>

#include "SpawnerInputStream.h"
#include "SpawnerOutputStream.h"


#include "jni.h"


void ThrowByName(JNIEnv *env, const char *name, const char *msg);

#define BUFF_SIZE  (1024)

/* Inaccessible static: skipBuffer */
/*
 * Class:     SpawnerInputStream
 * Method:    read0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_SpawnerInputStream_read0
  (JNIEnv * env, jobject proc, jint fd, jbyteArray buf, jint len)
{
	unsigned char tmpBuf[BUFF_SIZE];	
	int nBuffOffset = 0;
//	printf("Come to read0\n");
	while(len > nBuffOffset)
		{
		int nReadLen = min(len - nBuffOffset, BUFF_SIZE);
		int nread;
		nread = read(fd, tmpBuf, nReadLen);
		if(nread > 0)
			(*env) -> SetByteArrayRegion(env, buf, nBuffOffset, nReadLen, tmpBuf);
		else
			break;
		nBuffOffset += nread;
		if(nread != nReadLen)
			break;
		}
//    printf("Leave read with %i bytes read\n", nBuffOffset);
	return nBuffOffset; // This is a real full readed length

}

/*
 * Class:     SpawnerInputStream
 * Method:    close0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_SpawnerInputStream_close0
  (JNIEnv * env, jobject proc, jint fd)
{
	return close(fd);
}

/*
 * Class:     SpawnerOutputStream
 * Method:    write0
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_SpawnerOutputStream_write0
  (JNIEnv * env, jobject proc, jint fd, jbyteArray buf, jint len)
{
	unsigned char tmpBuf[BUFF_SIZE];	
	int nBuffOffset = 0;
//	int rc = 0;
//   printf("Come to write0, len = %i\n", len);
	while(len > nBuffOffset)
		{
		int nWriteLen = min(len - nBuffOffset, BUFF_SIZE);
		(*env) -> GetByteArrayRegion(env, buf, nBuffOffset, nWriteLen, tmpBuf);
//		printf("Write %i bytes; last byte = %#x\n", nWriteLen, (int)tmpBuf[nWriteLen - 1]);
		if(nWriteLen != write(fd, tmpBuf, nWriteLen)) 
			{
//     		printf("Error: written %i bytes; errno = %i, fd = %i, len = %i ", nWriteLen, errno, fd, rc);
			ThrowByName(env, "java/io/IOException", strerror(errno));
			}
		nBuffOffset += nWriteLen;
		}
	return 0;
}

/*
 * Class:     SpawnerOutputStream
 * Method:    close0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_eclipse_cdt_utils_spawner_SpawnerOutputStream_close0
  (JNIEnv * env, jobject proc, jint fd)
{
	return close(fd);	
}
