@echo off
chcp 65001 >nul
title مشروع خوارزميات - نظام إدارة المهام والمشاريع
cd /d "%~dp0"

echo ============================================
echo   تجميع ملفات Java...
echo ============================================
if not exist out mkdir out
javac -encoding UTF-8 -d out src\app\*.java
if errorlevel 1 (
    echo.
    echo [خطأ] فشل التجميع — تأكد من تثبيت JDK (javac) على الجهاز.
    pause
    exit /b 1
)
echo [تم] التجميع نجح.

echo ============================================
echo   تشغيل الخادم على http://localhost:8080
echo ============================================
start "" http://localhost:8080
java -cp out app.Main
pause
