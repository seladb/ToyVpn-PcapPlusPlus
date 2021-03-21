#!/bin/bash
set -e

API_VERSION=29
TARGETS="arm64-v8a armeabi-v7a x86 x86_64"

# set Script Name variable
SCRIPT=`basename ${BASH_SOURCE[0]}`

# help function
function HELP {
    echo -e \\n"Help documentation for ${SCRIPT}."
    echo ""
    echo -e "Basic usage: $SCRIPT [-h] [--ndk-path] [--pcapplusplus-path] [--target]"\\n
    echo "The following switches are recognized:"
    echo "--ndk-path             --The path of Android NDK, for example: '/opt/Android/Sdk/ndk/22.0.7026061'"
    echo "--pcapplusplus-path    --The path of PcapPlusPlus source code"
    echo "--target               --Build for specific target, should be one of: arm64-v8a, armeabi-v7a, x86, x86_64"
    echo "--help|-h              --Displays this help message and exits. No further actions are performed"
    echo ""
}

# these are all the possible switches
OPTS=`getopt -o h --long ndk-path:,pcapplusplus-path:,target: -- "$@"`

# if user put an illegal switch - print HELP and exit
if [ $? -ne 0 ]; then
    HELP
    exit 1
fi

eval set -- "$OPTS"

NDK_PATH=""
PCAPPLUSPLUS_PATH=""

# go over all switches
while true ; do
    case "$1" in
    # NDK path
    --ndk-path)
        NDK_PATH=$2
        if [ ! -d "$NDK_PATH" ]; then
            echo "NDK directory '$NDK_PATH' not found. Exiting..."
            exit 1
        fi
        shift 2 ;;

    # PcapPlusPlus path
    --pcapplusplus-path)
        PCAPPLUSPLUS_PATH=$2
        if [ ! -d "$PCAPPLUSPLUS_PATH" ]; then
            echo "PcapPlusPlus directory '$PCAPPLUSPLUS_PATH' not found. Exiting..."
            exit 1
        fi
        shift 2 ;;


    # Target
    --target)
        TARGETS=$2
        case "$TARGETS" in
        arm64-v8a|armeabi-v7a|x86|x86_64)
            ;;
        *)
            echo -e "Target must be one of: arm64-v8a, armeabi-v7a,x86, x86_64"
            exit 1
        esac
        shift 2 ;;

    # help switch - display help and exit
    -h|--help)
        HELP
        exit 0
        ;;

    # empty switch - just go on
    --)
        shift ; break ;;

    # illegal switch
    *)
        echo -e \\n"Option -$OPTARG not allowed."
        HELP
        exit 1
    esac
done

if [ -z "$NDK_PATH" ]; then
    echo "Please specify the NDK path using the '--ndk-path' switch. Exiting..."
    exit 1
fi

if [ -z "$PCAPPLUSPLUS_PATH" ]; then
    echo "Please specify the location of PcapPlusPlus source code using the '--pcapplusplus-path' swtich. Exiting..."
    exit 1
fi

LIBS_PATH=$(pwd)/app/libs

LIBPCAP_INCLUDE_DIR=$LIBS_PATH/libpcap-android/include
# LIBPCAP_LIB_DIR doesn't really matter if you only build PcapPlusPlus libs
LIBPCAP_LIB_DIR=$LIBS_PATH/libpcap-android/armeabi-v7a/29

for TARGET in ${TARGETS}
do
    cd $PCAPPLUSPLUS_PATH && ./configure-android.sh --ndk-path $NDK_PATH --target $TARGET --api $API_VERSION --libpcap-include-dir $LIBPCAP_INCLUDE_DIR --libpcap-lib-dir $LIBPCAP_LIB_DIR
    cd $PCAPPLUSPLUS_PATH && make clean && make libs
    PCAPPLUSPLUS_LIB_PATH=$LIBS_PATH/pcapplusplus/$TARGET/$API_VERSION
    mkdir -p $PCAPPLUSPLUS_LIB_PATH
    cp $PCAPPLUSPLUS_PATH/Dist/lib*.a $PCAPPLUSPLUS_LIB_PATH
done

PCAPPLUSPLUS_INCLUDE=$LIBS_PATH/pcapplusplus/include
mkdir -p $PCAPPLUSPLUS_INCLUDE
cp $PCAPPLUSPLUS_PATH/Dist/header/* $PCAPPLUSPLUS_INCLUDE

echo ""
echo "*************************************************************"
echo "Finished building PcapPlusPlus for targets: $TARGETS"
echo "*************************************************************"
echo ""
