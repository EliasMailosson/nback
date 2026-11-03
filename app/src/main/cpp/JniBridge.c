#include "jni.h"
#include "nback.h"
#include "nback.c"

static jobject nback_result(JNIEnv *env, const char *errorName, jintArray sequence) {
    jclass resultClass = (*env)->FindClass(
            env, "mobappdev/example/nback_cimpl/NBackGeneration");
    if (resultClass == NULL) {
        return NULL;
    }

    if (errorName != NULL) {
        jmethodID errorMethod = (*env)->GetStaticMethodID(
                env,
                resultClass,
                "error",
                "(Ljava/lang/String;)Lmobappdev/example/nback_cimpl/NBackGeneration;");
        if (errorMethod == NULL) {
            return NULL;
        }
        jstring name = (*env)->NewStringUTF(env, errorName);
        if (name == NULL) {
            return NULL;
        }
        return (*env)->CallStaticObjectMethod(env, resultClass, errorMethod, name);
    }

    jmethodID okMethod = (*env)->GetStaticMethodID(
            env,
            resultClass,
            "ok",
            "([I)Lmobappdev/example/nback_cimpl/NBackGeneration;");
    if (okMethod == NULL) {
        return NULL;
    }
    return (*env)->CallStaticObjectMethod(env, resultClass, okMethod, sequence);
}

JNIEXPORT jobject JNICALL
Java_mobappdev_example_nback_1cimpl_NBackHelper_createNBackString(
        JNIEnv *env, jobject thiz,
        jint size, jint stimuli, jint nBack, jint matches, jint seed) {
    (void) thiz;

    Nback sequence = NULL;
    const char *error = nback_create(
            size, stimuli, nBack, matches, (unsigned int) seed, &sequence);
    if (error != NULL) {
        nback_free(sequence);
        return nback_result(env, error, NULL);
    }

    int length = nback_length(sequence);
    const int *values = nback_values(sequence);
    jintArray array = (*env)->NewIntArray(env, length);
    if (array == NULL) {
        nback_free(sequence);
        return NULL;
    }

    (*env)->SetIntArrayRegion(env, array, 0, length, values);
    nback_free(sequence);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return nback_result(env, NULL, array);
}
