@echo off
REM One-command CI loop. Run this from the project root after Claude edits files.
REM Stages everything, commits with a timestamp, pushes, and prints the URL to
REM watch. Claude reads the run and its logs from the GitHub API afterwards.

setlocal
cd /d "%~dp0"

git add -A
git diff --cached --quiet && (
  echo Nothing to commit - working tree matches the last push.
  goto :end
)

for /f "tokens=* usebackq" %%t in (`powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm'"`) do set STAMP=%%t
git commit -m "wip %STAMP%"
git push

echo.
for /f "tokens=* usebackq" %%u in (`git remote get-url origin`) do set ORIGIN=%%u
echo Pushed. Actions: %ORIGIN:~0,-4%/actions
echo (if that URL looks odd, just open your repo and click the Actions tab)

:end
endlocal
