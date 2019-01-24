::inizio script
ECHO OFF

ECHO Si eliminano eventuali processi Client e Server ancora live da esecuzioni precedenti
Taskkill /IM java.exe /F


ECHO Si rimuovono eventuali documenti relativi ad esecuzioni di demo
rd /S /Q Server\Documento
rd /S /Q Server\Presentazione
rd /S /Q Server\Prova
rd /S /Q Server\Doc


ECHO Questa demo vuole mostrare un esempio delle funzionalita' base di TURING

cd Server
ECHO Il server va in esecuzione
START /B java MainClassTuringServer  > ..\log\output_server.txt 2>&1
TIMEOUT 5

cd ..\Client

ECHO Il client 1 inizia l'esecuzione
START /B java MainClassTuringClient <..\ClientInput\inputClient1.txt > ..\log\outputClient1.txt 2>&1

ECHO Esecuzione terminata. L'input dato al client si trova in inputClient1.txt, l'output restituito invece in outputClient1.txt

cd ..\



