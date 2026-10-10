#!/usr/bin/env sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/ontime-cpp-tests.XXXXXX")
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM

compiler=${CXX:-c++}
common_flags="-std=c++11 -Wall -Wextra -Werror -pedantic"

"$compiler" $common_flags "$repo_dir/tests/departure_policy_test.cpp" \
  -o "$build_dir/departure_policy_test"
"$build_dir/departure_policy_test"

"$compiler" $common_flags "$repo_dir/tests/datetime_parser_test.cpp" \
  "$repo_dir/ProchainMetro/src/transport/datetime_parser.cpp" \
  -o "$build_dir/datetime_parser_test"
"$build_dir/datetime_parser_test"
