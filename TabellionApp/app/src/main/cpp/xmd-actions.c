#include <string.h>
#include <jni.h>
#include <stdio.h>
#include <asm/fcntl.h>
#include <fcntl.h>
#include <errno.h>
#include <zconf.h>
#include <dirent.h>
#include <sys/stat.h>
#include <wait.h>
#include "pimage.h"

static processimage process;

JNIEXPORT jstring JNICALL
Java_edu_uci_ics_charm_tabellion_OperationsWithNDK_stringFromJNI( JNIEnv* env,
                                                  jobject obj )
{
    char* test = "hey there! This is a message from C!";

    return (*env) -> NewStringUTF(env, test);
}

int
sustem(char *cmd) {
    int ret = -1;
    pid_t child;

    switch((child = fork())) {
        case -1:
            return -1;

        case 0:
            return execlp("su", "su", "-c", cmd, NULL);

        default:
            waitpid(child, &ret, 0);
    }

    return ret;
}

JNIEXPORT jboolean JNICALL
Java_edu_uci_ics_charm_tabellion_OperationsWithNDK_sudo(JNIEnv *env, jobject obj, jstring xmd) {
    // This is the function for executing command lines in su(sudo) mode
    char const * const cmd = (*env)->GetStringUTFChars(env, xmd, 0), *rmd;
    jboolean ret = JNI_TRUE;
    if(process.pid) {
        for(rmd = cmd; *rmd; ++rmd) {
            if(write(process.infd, rmd, 1) != 1)
                ret = JNI_FALSE;
        }
        if(write(process.infd, "\n", 1) != 1)
            ret = JNI_FALSE;
    } else if(sustem(cmd))
        ret = JNI_FALSE;

    (*env)->ReleaseStringUTFChars(env, xmd, cmd);
    return ret;
}

JNIEXPORT void JNICALL
Java_edu_uci_ics_charm_tabellion_OperationsWithNDK_startshell(JNIEnv *env, jobject obj) {
    // This is the function for starting a new shell process in the background
    // We currently only allow one shell process in the background
    // This function is created because we do not want the mainthread to run any heavy task since it needs handle UI
    if(!process.pid)
        mkprocess("su", &process, 1, 0, 0);
}

JNIEXPORT void JNICALL
Java_edu_uci_ics_charm_tabellion_OperationsWithNDK_closeshell(JNIEnv *env, jobject obj) {
    // This is the function to close the shell in the background.
    if(process.pid) {
        rmprocess(&process);
    }
}
