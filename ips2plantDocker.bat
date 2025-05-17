set REPOS=%1
for /f "tokens=1,* delims= " %%a in ("%*") do set ALL_BUT_FIRST=%%b

docker run  --rm -v "%cd%:/wrk" -v "%REPOS%:/repos" alpine-ips2plant %ALL_BUT_FIRST%