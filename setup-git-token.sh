#!/bin/bash
# Git Token Configuration Script
# This script helps you configure git to use a personal access token

# Store your GitHub Personal Access Token here
# REPLACE WITH YOUR ACTUAL TOKEN
GITHUB_TOKEN="your_github_token_here"

# Configure git to use the token
# Option 1: Store token in git credential helper (RECOMMENDED - SECURE)
echo "Configuring git to use credential helper..."
git config --global credential.helper store

# Option 2: If you want to set token for just this repository
# git config --local credential.helper store

# Option 3: Use the token directly (less secure, not recommended)
# git remote set-url origin https://$GITHUB_TOKEN@github.com/anilhemnani/moments-manager.git

echo "Git credential helper configured!"
echo "Next time you push, enter:"
echo "  Username: anilhemnani"
echo "  Password: <your_github_token>"
echo ""
echo "Git will remember the token for future use."

