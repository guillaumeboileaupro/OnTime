#!/usr/bin/env sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/ontime-cpp-tests.XXXXXX")
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM

"${CXX:-c++}" -std=c++11 -Wall -Wextra -Werror -pedantic \
  "$repo_dir/tests/departure_policy_test.cpp" \
  -o "$build_dir/departure_policy_test"
"$build_dir/departure_policy_test"
