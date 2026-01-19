#!/bin/sh
# Setup Git hooks for APiGen development

echo "Setting up Git hooks..."

# Configure Git to use .githooks directory
git config core.hooksPath .githooks

# Make hooks executable
chmod +x .githooks/*

echo "✅ Git hooks configured successfully!"
echo ""
echo "The following hooks are now active:"
echo "  - pre-commit: Checks formatting and compilation"
echo ""
echo "To disable hooks temporarily, use: git commit --no-verify"
