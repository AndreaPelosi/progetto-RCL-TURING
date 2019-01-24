::inizio script
ECHO OFF

ECHO Si eliminano eventuali processi Client e Server ancora live da esecuzioni precedenti
Taskkill /IM java.exe /F


ECHO Si rimuovono eventuali documenti relativi ad esecuzioni di demo
rd /S /Q Server\Documento
rd /S /Q Server\Presentazione
rd /S /Q Server\Prova
rd /S /Q Server\Doc


ECHO Questa demo vuole mostrare che un utente puo' modificare un documento a seguito di un invito di un altro utente

cd Server
ECHO Il server va in esecuzione
START /B java MainClassTuringServer  > ..\log\output_server.txt 2>&1
TIMEOUT 5

cd ..\Client


ECHO Il client 2 inizia l'esecuzione
START /B java MainClassTuringClient <..\ClientInput\inputClient2.txt > ..\log\outputClient2.txt 2>&1

TIMEOUT 3

ECHO Il client 3 inizia l'esecuzione
START /B java MainClassTuringClient <..\ClientInput\inputClient3.txt > ..\log\outputClient3.txt 2>&1

ECHO Esecuzione terminata. L'input dato al client 2 si trova in inputClient2.txt, l'output restituito invece in outputClient2.txt, analogamente per il client 3

cd ..\



