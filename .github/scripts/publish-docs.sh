#!/usr/bin/env bash
# Adds the documentation of every released version that doesn't have it yet.
#
# A version counts as released once its POM is on Maven Central. Each new version is
# built from its tag into <pages-dir>/<version>/ and is not rebuilt afterwards, unless
# named as the version to rebuild. versions.json, latest/ and the root redirect are then
# regenerated from the version folders.
#
# Usage: publish-docs.sh <pages-dir> [version-to-rebuild]
# For a local preview without a release, set SKIP_CENTRAL_CHECK=true.
set -euo pipefail

pages_dir=$(cd "$1" && pwd)
rebuild=${2:-}
repo_root=$(git rev-parse --show-toplevel)
central=https://repo1.maven.org/maven2/dev/abstraction4j/router/abstraction4j-router-core
changed=false

released() {
    [[ "${SKIP_CENTRAL_CHECK:-false}" == true ]] && return 0
    curl --silent --fail --head --location "$central/$1/abstraction4j-router-core-$1.pom" > /dev/null
}

build() {
    local version=$1 worktree
    worktree=$(mktemp -d)
    git -C "$repo_root" worktree add --quiet --detach "$worktree" "v$version"
    (cd "$worktree" && ./gradlew --quiet site -Pversion="$version")
    rm -rf "${pages_dir:?}/$version"
    cp -R "$worktree/build/site" "$pages_dir/$version"
    git -C "$repo_root" worktree remove --force "$worktree"
}

for tag in $(git -C "$repo_root" tag --list 'v*' --sort=version:refname); do
    version=${tag#v}
    if [[ -d "$pages_dir/$version" && "$version" != "$rebuild" ]]; then
        continue
    fi
    if ! released "$version"; then
        echo "$version: not on Maven Central yet"
        continue
    fi
    echo "$version: building documentation"
    build "$version"
    changed=true
done

# Newest first; "latest" is the newest version without a pre-release suffix.
versions=$(find "$pages_dir" -mindepth 1 -maxdepth 1 -type d -name '[0-9]*' -exec basename {} \; | sort --version-sort --reverse)
latest=$(grep -v -- '-' <<< "$versions" | head -n 1 || true)
latest=${latest:-$(head -n 1 <<< "$versions")}

if [[ -n "$latest" ]]; then
    {
        printf '{\n  "latest": "%s",\n  "versions": [' "$latest"
        sep=""
        for v in $versions; do
            printf '%s"%s"' "$sep" "$v"
            sep=", "
        done
        printf ']\n}\n'
    } > "$pages_dir/versions.json"

    redirect() {
        printf '<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>Abstraction4j Router</title>\n<meta http-equiv="refresh" content="0; url=%s">\n<link rel="canonical" href="%s">\n</head>\n<body>\n<p><a href="%s">Abstraction4j Router %s documentation</a></p>\n</body>\n</html>\n' "$1" "$1" "$1" "$latest"
    }
    mkdir -p "$pages_dir/latest"
    redirect "$latest/README.html" > "$pages_dir/index.html"
    redirect "../$latest/README.html" > "$pages_dir/latest/index.html"
    touch "$pages_dir/.nojekyll"
    echo "Published versions: $(tr '\n' ' ' <<< "$versions")(latest $latest)"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "changed=$changed" >> "$GITHUB_OUTPUT"
    echo "published=$([[ -n "$latest" ]] && echo true || echo false)" >> "$GITHUB_OUTPUT"
fi
