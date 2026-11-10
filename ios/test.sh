#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
test_dir=$(mktemp -d)
trap 'rm -rf "$test_dir"' EXIT
"${CC:-cc}" -std=c11 -Wall -Wextra -Werror -pedantic -Isrc \
    src/MediaRoutes.c tests/MediaRoutesTest.c -o "$test_dir/media-routes"
"$test_dir/media-routes"
