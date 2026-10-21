#!/bin/sh
# Fetches lucide icons and converts them to Android vector drawables (stroke-only, 24x24 viewport).
# The original web app draws lucide-react 0.546 icons with `fill=none stroke=currentColor stroke-width=2
# stroke-linecap=round stroke-linejoin=round`, which is exactly what {S} below reproduces.
#
# usage: tools/icons/lucide.sh [name...]        (defaults to the list below)
set -e
cd "$(dirname "$0")/../.."
OUT=app/src/main/res/drawable
VER=0.546.0

NAMES="${*:-filter clock briefcase file-spreadsheet chevron-left chevron-right tag rotate-ccw user printer folder-kanban folder-open folder-tree list-filter}"

mkdir -p "$OUT" .icons
for n in $NAMES; do
    f=".icons/$n.svg"
    [ -s "$f" ] || curl -fsS --max-time 30 "https://unpkg.com/lucide-static@$VER/icons/$n.svg" -o "$f"
    # lucide-static names are kebab-case; android resources cannot contain '-'
    res=$(echo "$n" | tr '-' '_')
    awk -v name="$n" '
        function attr(s, key,   re, v) {
            re = key "=\"[^\"]*\""
            if (match(s, re) == 0) return ""
            v = substr(s, RSTART, RLENGTH)
            sub(key "=\"", "", v); sub(/"$/, "", v)
            return v
        }
        BEGIN {
            S = " android:strokeColor=\"#FF000000\" android:strokeWidth=\"2\" android:strokeLineCap=\"round\" android:strokeLineJoin=\"round\""
            print "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\""
            print "    android:width=\"24dp\""
            print "    android:height=\"24dp\""
            print "    android:viewportWidth=\"24\""
            print "    android:viewportHeight=\"24\">"
        }
        {
            line = $0
            tag = line
            sub(/^[ \t]*</, "", tag); sub(/[ \t\/>].*$/, "", tag)
            if (tag == "path") {
                print "    <path" S " android:pathData=\"" attr(line, "d") "\"/>"
            } else if (tag == "circle") {
                cx = attr(line, "cx") + 0; cy = attr(line, "cy") + 0; r = attr(line, "r") + 0
                printf "    <path%s android:pathData=\"M%.3f %.3fa%.3f %.3f 0 1 0 %.3f 0a%.3f %.3f 0 1 0 -%.3f 0z\"/>\n", S, cx - r, cy, r, r, 2 * r, r, r, 2 * r
            } else if (tag == "line") {
                printf "    <path%s android:pathData=\"M%s %sL%s %s\"/>\n", S, attr(line, "x1"), attr(line, "y1"), attr(line, "x2"), attr(line, "y2")
            } else if (tag == "rect") {
                x = attr(line, "x") + 0; y = attr(line, "y") + 0; w = attr(line, "width") + 0; h = attr(line, "height") + 0; rx = attr(line, "rx") + 0
                if (rx == 0) {
                    printf "    <path%s android:pathData=\"M%.3f %.3fh%.3fv%.3fh-%.3fz\"/>\n", S, x, y, w, h, w
                } else {
                    printf "    <path%s android:pathData=\"M%.3f %.3fa%.3f %.3f 0 0 1 %.3f 0h%.3fa%.3f %.3f 0 0 1 0 %.3fh-%.3fa%.3f %.3f 0 0 1 -%.3f 0h-%.3fa%.3f %.3f 0 0 1 0 -%.3fz\"/>\n", \
                        S, x + rx, y, rx, rx, w - 2 * rx, w - 2 * rx, rx, rx, h - 2 * rx, w, w - 2 * rx, rx, rx, -(w - 2 * rx), h - 2 * rx, rx, rx, -(h - 2 * rx)
                }
            } else if (tag == "polyline" || tag == "polygon") {
                pts = attr(line, "points"); gsub(/[ \t]+/, " ", pts); gsub(/^ | $/, "", pts)
                n = split(pts, p, " ")
                d = "M" p[1] " " p[2]
                for (i = 3; i + 1 <= n; i += 2) d = d "L" p[i] " " p[i + 1]
                if (tag == "polygon") d = d "z"
                printf "    <path%s android:pathData=\"%s\"/>\n", S, d
            }
        }
        END { print "</vector>" }
    ' "$f" > "$OUT/ic_lucide_$res.xml"
    echo "wrote $OUT/ic_lucide_$res.xml"
done
