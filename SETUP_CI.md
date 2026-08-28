# One-time CI setup

Run these once, from `C:\Users\cesga\Claude\Projects\crystalkey`.
After this, every iteration costs you one command: `push`.

## 1 · Create the repo on GitHub

Open <https://github.com/new> and create a **public** repo named `crystalkey`.
Do **not** tick "Add a README", ".gitignore" or "license" — the folder already
has them and an initialised repo makes the first push conflict.

## 2 · Push what is on disk

```bat
cd /d C:\Users\cesga\Claude\Projects\crystalkey

git init
git branch -M main
git add -A
git commit -m "Crystal Key: verified core, Compose scaffold, CI"
git remote add origin https://github.com/YOUR-USERNAME/crystalkey.git
git push -u origin main
```

Replace `YOUR-USERNAME`. If git asks who you are:

```bat
git config user.email "cesgab1@gmail.com"
git config user.name  "Cesar"
```

`local.properties`, `.idea/`, `.gradle/` and `build/` are already gitignored,
so your SDK path and IDE state stay off GitHub.

## 3 · Confirm Actions is on

Open the repo's **Actions** tab. A run named **build** should already be going
from that first push. If GitHub asks you to enable workflows for the repo, say
yes.

That is the whole setup.

## Every iteration after this

```bat
push
```

That is `push.bat` in the project root: stages, commits with a timestamp,
pushes, and prints the Actions URL. Nothing else to remember.

## What CI actually runs

Three jobs in `.github/workflows/build.yml`:

| Job | What it proves | Time |
|---|---|---|
| `core` | The 31-check rule suite on the JVM | ~1 min |
| `assemble` | `:app` compiles; uploads `app-debug.apk` as an artifact | ~3 min |
| `instrumented` | The Compose UI tests on a real API-34 emulator | ~8 min |

The `assemble` job is the one that matters right now — it is the first thing in
this project's life that will compile the Compose layer end to end.

## Why public

GitHub serves workflow runs and job logs for public repos to anonymous callers,
and `api.github.com` is reachable from Claude's container. So Claude reads the
full build output directly — every error at once, in order, rather than whatever
fragment gets pasted into chat. No token, and no credential in the transcript.

If you later want it private, that trade reverses: Claude would need a
fine-grained PAT (Contents + Actions, that repo only, short expiry) pasted into
the chat, and it would live in the transcript until you revoke it.
