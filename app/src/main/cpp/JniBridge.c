#include "jni.h"
#include "nback.h"
#include "nback.c"

/* NULL with no pending exception means the error name could not be allocated.
   NBackHelper maps that null to ALLOCATION. A pending exception is left
   in place so Kotlin receives it, for example NoSuchMethodError. */
static jobject generation_error(JNIEnv *env, jclass resultClass, jmethodID errorMethod,
                                const char *errorName) {
    jstring name = (*env)->NewStringUTF(env, errorName);
    if (name == NULL) {
        (*env)->ExceptionClear(env);
        return NULL;
    }

    jobject result = (*env)->CallStaticObjectMethod(env, resultClass, errorMethod, name);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return result;
}

JNIEXPORT jobject JNICALL
Java_mobappdev_example_nback_1cimpl_NBackHelper_createNBackString(
        JNIEnv *env, jobject thiz,
        jint size, jint stimuli, jint nBack, jint matches, jint seed) {
    (void) thiz;

    /* Look up the result type before a sequence exists, so a JNI failure
       never reads values that C has not finished. */
    jclass resultClass = (*env)->FindClass(
            env, "mobappdev/example/nback_cimpl/NBackGeneration");
    if (resultClass == NULL) {
        return NULL;
    }

    jmethodID errorMethod = (*env)->GetStaticMethodID(
            env,
            resultClass,
            "error",
            "(Ljava/lang/String;)Lmobappdev/example/nback_cimpl/NBackGeneration;");
    if (errorMethod == NULL) {
        return NULL;
    }

    jmethodID okMethod = (*env)->GetStaticMethodID(
            env,
            resultClass,
            "ok",
            "([I)Lmobappdev/example/nback_cimpl/NBackGeneration;");
    if (okMethod == NULL) {
        return NULL;
    }

    Nback sequence = NULL;
    const char *error = nback_create(
            size, stimuli, nBack, matches, (unsigned int) seed, &sequence);
    if (error != NULL) {
        nback_free(sequence);
        return generation_error(env, resultClass, errorMethod, error);
    }

    int length = nback_length(sequence);
    const int *values = nback_values(sequence);
    jintArray array = (*env)->NewIntArray(env, length);
    if (array == NULL) {
        nback_free(sequence);
        (*env)->ExceptionClear(env);
        return generation_error(env, resultClass, errorMethod, NBACK_ERROR_ALLOCATION);
    }

    (*env)->SetIntArrayRegion(env, array, 0, length, values);
    nback_free(sequence);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        return generation_error(env, resultClass, errorMethod, NBACK_ERROR_ALLOCATION);
    }

    jobject result = (*env)->CallStaticObjectMethod(env, resultClass, okMethod, array);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return result;
}
