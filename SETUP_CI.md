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
git remote add origin https://github.com/cesgab1/crystalkey.git
git push -u origin main
```

If `origin` already exists from an earlier attempt, point it at the right place
instead of adding it again:

```bat
git remote set-url origin https://github.com/cesgab1/crystalkey.git
git remote -v
```

If git asks who you are:

```bat
git config user.email "cesgab1@gmail.com"
git config user.name  "Cesar"
```

`local.properties`, `.idea/`, `.gradle/` and `build/` are already gitignored,
so your SDK path and IDE state stay off GitHub.

### Authentication

GitHub does not accept account passwords over HTTPS. On Windows, Git Credential
Manager ships with Git and opens a browser window to sign in — do that once and
it is remembered. If the terminal prompts for a *password* instead, that path
will fail; use a personal access token as the password, or install Git for
Windows so the credential manager handles it.

`remote: Repository not found` on a push almost always means one of two things:
the repo has not been created on GitHub yet, or the URL still has a placeholder
in it. It is rarely about permissions on a public repo.

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
