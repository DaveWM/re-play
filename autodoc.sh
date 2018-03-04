#!/bin/bash

# Command that generates the HTML.
export AUTODOC_CMD="lein codox"

# The directory where the result of $AUTODOC_CMD, the generated HTML, ends up. This
# is what gets committed to $AUTODOC_BRANCH.
export AUTODOC_DIR="target/doc"

# The git remote to fetch from and push to.
# export AUTODOC_REMOTE="origin"

# Branch name to commit and push to
# export AUTODOC_BRANCH="gh-pages"

\curl -sSL https://raw.githubusercontent.com/plexus/autodoc/master/autodoc.sh | bash
