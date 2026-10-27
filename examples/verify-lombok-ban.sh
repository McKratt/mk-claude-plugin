#!/usr/bin/env bash
# Checks that each example project's lombok.config rejects @Builder, @Data and @Value
# while still accepting an allowed Lombok annotation (@Getter).
# Usage: examples/verify-lombok-ban.sh   (needs JDK 25 and Maven 3.10+ on the PATH)
set -uo pipefail

root="$(cd "$(dirname "$0")" && pwd)"
status=0

probe() { # project, annotation, class name, extra body
  local project="$1" annotation="$2" name="$3" body="$4"
  local dir="$root/$project/src/main/java/com/example/shop/configuration"
  local file="$dir/$name.java"
  cat > "$file" <<JAVA
package com.example.shop.configuration;

$annotation
class $name {
$body
}
JAVA
  (cd "$root/$project" && mvn -B -ntp -q compile >/dev/null 2>&1)
  local rc=$?
  rm -f "$file"
  return $rc
}

for project in single-context multi-context helidon-single-context; do
  echo "== $project"
  if probe "$project" "@lombok.Getter" "ProbeAllowed" "    private String value = \"x\";"; then
    echo "  OK    @Getter compiles"
  else
    echo "  FAIL  @Getter should compile"; status=1
  fi
  for ann in Builder Data Value; do
    if probe "$project" "@lombok.$ann" "Probe$ann" "    private String value;"; then
      echo "  FAIL  @$ann compiled but must be rejected"; status=1
    else
      echo "  OK    @$ann rejected"
    fi
  done
done
exit $status
