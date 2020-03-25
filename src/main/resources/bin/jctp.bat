@ echo off
title jctp
@ echo path is %~dp0
@ pushd "%~dp0..\"
@ PATH=%PATH%;dll
@ echo %JAVA_HOME%
@ set JAVA_OPT= -Xms5g -Xmx10g -XX:MaxPermSize=128M -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:gc.log
@ echo %JAVA_OPT%
@ "%JAVA_HOME%\bin\java.exe" %JAVA_OPT% -cp "conf/;lib/*" market.futures.JctpApplication
@ popd
@ pause