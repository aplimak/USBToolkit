//
// Created by aeliux on 9/19/26.
//

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <android/log.h>
#include <magic.h>

#define LOG_TAG "libusbtoolkit.magic"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)

#define MAGIC_RESULT_CLASS "ir/aeliux/usbtoolkit/data/MagicResult"

/* One cookie per output mode — libmagic bakes the mode bits into the
 * cookie at magic_open() time, so a single cookie cannot produce both
 * MIME and human-readable output. */
static magic_t g_cookie_mime = NULL;   /* MAGIC_MIME_TYPE     */
static magic_t g_cookie_enc  = NULL;   /* MAGIC_MIME_ENCODING */
static magic_t g_cookie_desc = NULL;   /* MAGIC_NONE          */

/* magic_t is not thread-safe; serialize all access. */
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

static magic_t open_and_load(const char *db_path, int mode)
{
    magic_t c = magic_open(mode | MAGIC_ERROR | MAGIC_SYMLINK);
    if (c == NULL) return NULL;
    if (magic_load(c, db_path) != 0) {
        LOGE("magic_load(%s) failed: %s", db_path, magic_error(c));
        magic_close(c);
        return NULL;
    }
    return c;
}

/* public static native void magicInit(String dbPath) */
JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_Native_magicInit(
        JNIEnv *env, jclass cls, jstring j_dbPath)
{
    if (j_dbPath == NULL) {
        LOGE("magicInit: dbPath is null");
        return;
    }

    pthread_mutex_lock(&g_lock);

    if (g_cookie_desc != NULL) {        /* already initialized */
        pthread_mutex_unlock(&g_lock);
        return;
    }

    const char *dbPath = (*env)->GetStringUTFChars(env, j_dbPath, NULL);
    if (dbPath == NULL) {
        pthread_mutex_unlock(&g_lock);
        return;
    }

    g_cookie_desc = open_and_load(dbPath, MAGIC_NONE);
    g_cookie_mime = open_and_load(dbPath, MAGIC_MIME_TYPE);
    g_cookie_enc  = open_and_load(dbPath, MAGIC_MIME_ENCODING);

    (*env)->ReleaseStringUTFChars(env, j_dbPath, dbPath);

    if (g_cookie_desc == NULL || g_cookie_mime == NULL || g_cookie_enc == NULL) {
        LOGE("magicInit: one or more cookies failed to load");
        /* leave whatever succeeded in place; release() cleans it up */
    } else {
        LOGI("magicInit: libmagic initialized");
    }

    pthread_mutex_unlock(&g_lock);
}

/* Build a MagicResult(String, String, String, String). Any arg may be NULL. */
static jobject build_result(JNIEnv *env,
                            const char *mime,
                            const char *desc,
                            const char *enc,
                            const char *err)
{
    jclass cls = (*env)->FindClass(env, MAGIC_RESULT_CLASS);
    if (cls == NULL) {
        LOGE("MagicResult class not found");
        return NULL;
    }

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                                         "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (ctor == NULL) {
        (*env)->DeleteLocalRef(env, cls);
        return NULL;
    }

    jstring jm = mime ? (*env)->NewStringUTF(env, mime) : NULL;
    jstring jd = desc ? (*env)->NewStringUTF(env, desc) : NULL;
    jstring je = enc  ? (*env)->NewStringUTF(env, enc)  : NULL;
    jstring jr = err  ? (*env)->NewStringUTF(env, err)  : NULL;

    jobject obj = (*env)->NewObject(env, cls, ctor, jm, jd, je, jr);

    if (jm) (*env)->DeleteLocalRef(env, jm);
    if (jd) (*env)->DeleteLocalRef(env, jd);
    if (je) (*env)->DeleteLocalRef(env, je);
    if (jr) (*env)->DeleteLocalRef(env, jr);
    (*env)->DeleteLocalRef(env, cls);

    return obj;
}

/* public static native MagicResult magicAnalyzeFile(String path) */
JNIEXPORT jobject JNICALL
Java_ir_aeliux_usbtoolkit_Native_magicAnalyzeFile(
        JNIEnv *env, jclass cls, jstring j_path)
{
    if (j_path == NULL) {
        return build_result(env, NULL, NULL, NULL, "path is null");
    }

    pthread_mutex_lock(&g_lock);

    if (g_cookie_desc == NULL || g_cookie_mime == NULL || g_cookie_enc == NULL) {
        pthread_mutex_unlock(&g_lock);
        return build_result(env, NULL, NULL, NULL, "libmagic not initialized");
    }

    const char *path = (*env)->GetStringUTFChars(env, j_path, NULL);
    if (path == NULL) {
        pthread_mutex_unlock(&g_lock);
        return build_result(env, NULL, NULL, NULL, "GetStringUTFChars failed");
    }

    const char *mime = magic_file(g_cookie_mime, path);
    const char *enc  = magic_file(g_cookie_enc,  path);
    const char *desc = magic_file(g_cookie_desc, path);

    char *mime_c = mime ? strdup(mime) : NULL;
    char *enc_c  = enc  ? strdup(enc)  : NULL;
    char *desc_c = desc ? strdup(desc) : NULL;

    /* error is shared across cookies; use the description cookie */
    const char *err = magic_error(g_cookie_desc);
    char *err_c = (err && *err) ? strdup(err) : NULL;

    (*env)->ReleaseStringUTFChars(env, j_path, path);
    pthread_mutex_unlock(&g_lock);

    jobject result = build_result(env, mime_c, desc_c, enc_c, err_c);

    free(mime_c);
    free(enc_c);
    free(desc_c);
    free(err_c);

    return result;
}

/* public static native void magicRelease() */
JNIEXPORT void JNICALL
Java_ir_aeliux_usbtoolkit_Native_magicRelease(JNIEnv *env, jclass cls)
{
    pthread_mutex_lock(&g_lock);

    if (g_cookie_mime) { magic_close(g_cookie_mime); g_cookie_mime = NULL; }
    if (g_cookie_enc)  { magic_close(g_cookie_enc);  g_cookie_enc  = NULL; }
    if (g_cookie_desc) { magic_close(g_cookie_desc); g_cookie_desc = NULL; }

    pthread_mutex_unlock(&g_lock);
    LOGI("magicRelease: libmagic released");
}