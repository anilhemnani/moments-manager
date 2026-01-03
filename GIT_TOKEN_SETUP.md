# How to Use Your GitHub Personal Access Token

## Option 1: Credential Helper (RECOMMENDED - Most Secure)

### Step 1: Configure Git Credential Helper
```bash
git config --global credential.helper store
```

### Step 2: Push Your Changes
```bash
cd /home/anilhemnani/moments-manager
git push --set-upstream origin master
```

### Step 3: When Prompted
```
Username: anilhemnani
Password: <paste your GitHub Personal Access Token here>
```

Git will automatically store and remember the credentials for future use.

### Step 4: Verify It Works
```bash
git push
# Should push without asking for credentials again
```

---

## Option 2: Store Token in Environment Variable

### Step 1: Create .env.local file (ignored by git)
```bash
echo "GITHUB_TOKEN=your_actual_token_here" > .env.local
```

### Step 2: Load and use in script
```bash
source .env.local
git push https://$GITHUB_TOKEN@github.com/anilhemnani/moments-manager.git
```

---

## Option 3: Store in git Config (Repository-Only)

### For this repository only:
```bash
git config --local credential.helper store
git push --set-upstream origin master
# Then enter token when prompted
```

### For all repositories globally:
```bash
git config --global credential.helper store
git push --set-upstream origin master
# Then enter token when prompted
```

---

## Security Best Practices

### ✅ DO THIS:
- ✅ Use GitHub Personal Access Token (not your password)
- ✅ Store token in `.env` or `.env.local` (both in .gitignore)
- ✅ Use credential helper to store token securely
- ✅ Give token minimal required permissions
- ✅ Regenerate token periodically
- ✅ Revoke old tokens after rotating

### ❌ DON'T DO THIS:
- ❌ Don't commit token to Git
- ❌ Don't share token via email/chat
- ❌ Don't use your GitHub password in git commands
- ❌ Don't set token expiration too far in future
- ❌ Don't give token more permissions than needed

---

## Token Already in .gitignore

These files are protected and won't be committed:
```
git-token.txt
git-credentials
.git-credentials
*.token
.env
.env.local
credentials.txt
```

---

## Quick Setup Command

```bash
# Configure credential helper (stores credentials securely)
git config --global credential.helper store

# Push your code
cd /home/anilhemnani/moments-manager
git push --set-upstream origin master

# When prompted:
# Username: anilhemnani
# Password: <your_github_token>

# Done! Git will remember it for future use.
```

---

## Verify Your Token Works

```bash
# Test if push works
git push

# If it works, you should see:
# Everything up-to-date
# OR
# X files changed, Y insertions(+), Z deletions(-)
```

---

## If You Need to Change Token Later

```bash
# Clear stored credentials
git credential-manager-core erase

# Or edit credential file directly
nano ~/.git-credentials

# Then configure again
git config --global credential.helper store
git push  # Re-enter new token when prompted
```

---

## Your Repository Setup

```
Repository: moments-manager
Owner: anilhemnani
Remote URL: https://github.com/anilhemnani/moments-manager.git
Token stored: Secure credential helper
```

---

**Status:** ✅ Ready to push code with GitHub Personal Access Token

