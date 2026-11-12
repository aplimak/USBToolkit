//
// Created by aeliux on 9/19/26.
//

#define LOG_TAG "libmagic:"__FILE_NAME__

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <magic.h>
#include <axsn/log.h>
#include <axsn/error.h>
#include <ptrw.h>

#define MAGIC_RESULT_CLASS "ir/aeliux/magic/MagicResult"

JNIEXPORT jlong JNICALL
Java_ir_aeliux_magic_Native_init(
        JNIEnv *env, jclass cls, jstring j_dbPath)
{
    if (j_dbPath == NULL) {
        AXSN_THROW_IAE(env, "dbPath cannot be null");
        return 0;
    }

    const char *dbPath = (*env)->GetStringUTFChars(env, j_dbPath, NULL);
    if (dbPath == NULL) {
        return 0;
    }

    magic_t magic = magic_open(MAGIC_MIME_TYPE | MAGIC_ERROR | MAGIC_SYMLINK);
    if (magic == NULL) {
        AXSN_THROW_RTE(env, "magic_open failed");
        goto clean;
    }
    if (magic_load(magic, dbPath) != 0) {
        AXSN_THROW_RTE(env, "magic_load(%s) failed: %s", dbPath, magic_error(magic));
        goto fail;
    }

    LOGV("magic_open -> %p", (void *) magic);

    goto clean;

    fail:
    if (magic) magic_close(magic);
    clean:
    if (dbPath) (*env)->ReleaseStringUTFChars(env, j_dbPath, dbPath);

    return magic ? ptrw_wrap(env, magic) : 0;
}

static jobject build_result(JNIEnv *env,
                            const char *mime,
                            const char *err)
{
    jclass cls = (*env)->FindClass(env, MAGIC_RESULT_CLASS);
    if (cls == NULL) {
        return NULL;
    }

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                                         "(Ljava/lang/String;Ljava/lang/String;)V");
    if (ctor == NULL) {
        (*env)->DeleteLocalRef(env, cls);
        return NULL;
    }

    jstring jm = mime ? (*env)->NewStringUTF(env, mime) : NULL;
    jstring jr = err ? (*env)->NewStringUTF(env, err) : NULL;

    jobject obj = (*env)->NewObject(env, cls, ctor, jm, jr);

    if (jm) (*env)->DeleteLocalRef(env, jm);
    if (jr) (*env)->DeleteLocalRef(env, jr);
    (*env)->DeleteLocalRef(env, cls);

    return obj;
}

JNIEXPORT jobject JNICALL
Java_ir_aeliux_magic_Native_analyzeFile(
        JNIEnv *env, jclass cls, jlong j_magic, jstring j_path)
{
    magic_t magic = ptrw_unwrap(env, j_magic);
    if (magic == NULL) {
        return NULL;
    }

    if (j_path == NULL) {
        AXSN_THROW_IAE(env, "path cannot be null");
        return NULL;
    }

    const char *path = (*env)->GetStringUTFChars(env, j_path, NULL);
    if (path == NULL) {
        return NULL;
    }

    const char *mime = magic_file(magic, path);

    (*env)->ReleaseStringUTFChars(env, j_path, path);

    char *mime_c = mime ? strdup(mime) : NULL;

    /* error is shared across cookies; use the mime cookie */
    const char *err = magic_error(magic);
    char *err_c = (err && *err) ? strdup(err) : NULL;

    jobject result = build_result(env, mime_c, err_c);

    free(mime_c);
    free(err_c);

    return result;
}

JNIEXPORT void JNICALL
Java_ir_aeliux_magic_Native_release(JNIEnv *env, jclass cls, jlong j_magic)
{
    magic_t magic = ptrw_pop(env, j_magic);
    if (magic == NULL) {
        return;
    }

    LOGV("magic_close(%p)", (void *) magic);
    magic_close(magic);
}
