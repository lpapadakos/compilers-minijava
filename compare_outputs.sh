#!/bin/bash

shopt -s nocasematch

rm -rf output/
java Main "$@" &&
for JAVAFILE in "$@"; do
	BASENAME="${JAVAFILE##*/}"

	# Only java files
	[[ "$BASENAME" == *.java ]] || continue

	# Skip deliberate error cases
	[[ "$BASENAME" =~ "error" ]] && continue

	# Compare output of java execution vs LLVM compiled executable
	echo "$BASENAME"
	diff --color --side-by-side <(java "$JAVAFILE") <(./output/"${BASENAME%.*}") # || break
	echo
done
