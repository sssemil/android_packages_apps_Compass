LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := Compass2

LOCAL_STATIC_JAVA_LIBRARIES := \
        google-play-services

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := google-play-services:libs/google-play-services.jar

include $(BUILD_MULTI_PREBUILT)
