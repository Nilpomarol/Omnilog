"""Compares objective totals before and after the cumulative-to-increment conversion.

Reads a schema-7 (pre-increment) Omnilog backup and, for every ProgressUnits objective in it,
computes the total the old calculator produced and the total the new one produces after conversion.
Sessions whose contribution changed are listed, so a discrepancy points at data rather than a guess.

    python tools/diff_objective_totals.py <backup.json>
"""

import json
import sys
from collections import defaultdict

EPOCH_DAY_OFFSET = 719162  # days from 0001-01-01 to 1970-01-01, for readable dates


def epoch_day_to_date(day):
    import datetime
    return (datetime.date(1970, 1, 1) + datetime.timedelta(days=int(day))).isoformat()


def convert_session_to_increments(rows):
    """Mirror of ProgressIncrementConversion.convertSessionToIncrements."""
    if not rows:
        return 0, [], []

    ordered = sorted(rows, key=lambda r: (r["loggedAtEpochDay"], r["createdAtEpochMillis"], r["id"]))

    leading = []
    for r in ordered:
        if r["countsTowardObjectives"]:
            break
        leading.append(r)
    baseline = leading[-1]["progressValue"] if leading else 0

    deleted = [r["id"] for r in leading]
    entries = []
    previous = baseline
    for r in ordered[len(leading):]:
        amount = r["progressValue"] - previous
        previous = r["progressValue"]
        if amount <= 0:
            deleted.append(r["id"])
            continue
        entries.append({
            "id": r["id"],
            "amount": amount,
            "loggedAtEpochDay": r["loggedAtEpochDay"],
            "hasKnownDate": r["hasKnownDate"] and r["countsTowardObjectives"],
        })

    target = max(ordered[-1]["progressValue"] - baseline, 0)
    surplus = sum(e["amount"] for e in entries) - target
    if surplus > 0:
        trimmed = []
        for entry in reversed(entries):
            if surplus <= 0:
                trimmed.insert(0, entry)
            elif surplus >= entry["amount"]:
                surplus -= entry["amount"]
                deleted.append(entry["id"])
            else:
                entry = dict(entry, amount=entry["amount"] - surplus)
                trimmed.insert(0, entry)
                surplus = 0
        entries = trimmed

    return baseline, entries, deleted


def old_total_for_session(rows, start, end, ):
    """Mirror of the pre-change ObjectiveCalculator: derive deltas from cumulative values."""
    ordered = sorted(rows, key=lambda r: (r["loggedAtEpochDay"], r["createdAtEpochMillis"], r["id"]))
    previous = 0
    total = 0
    for r in ordered:
        delta = max(r["progressValue"] - previous, 0)
        previous = r["progressValue"]
        if r["countsTowardObjectives"] and r["hasKnownDate"] and start <= r["loggedAtEpochDay"] <= end:
            total += delta
    return total


def new_total_for_session(rows, start, end):
    _, entries, _ = convert_session_to_increments(rows)
    return sum(
        e["amount"] for e in entries
        if e["hasKnownDate"] and start <= e["loggedAtEpochDay"] <= end
    )


def main(path):
    backup = json.load(open(path, encoding="utf-8"))
    version = backup.get("schemaVersion")
    if version is None or version >= 8:
        print("This expects a pre-increment backup (schemaVersion < 8); got %s" % version)
        return

    items_by_id = {i["id"]: i for i in backup.get("mediaItems", [])}
    sessions_by_id = {s["id"]: s for s in backup.get("trackingSessions", [])}

    rows_by_session = defaultdict(list)
    for u in backup.get("progressUpdates", []):
        u.setdefault("hasKnownDate", True)
        u.setdefault("countsTowardObjectives", True)
        u.setdefault("createdAtEpochMillis", 0)
        rows_by_session[u["sessionId"]].append(u)

    objectives = [o for o in backup.get("objectives", []) if o.get("metric") == "ProgressUnits"]
    if not objectives:
        print("No ProgressUnits objectives in this backup.")
        return

    for obj in objectives:
        start = obj["startDateEpochDay"]
        end = obj["endDateEpochDay"]
        media_type = obj.get("mediaType")

        print("=" * 72)
        print("Objective: %s %s  %s to %s  target=%s  type=%s" % (
            obj.get("metric"), obj.get("unit"),
            epoch_day_to_date(start), epoch_day_to_date(end),
            obj.get("targetValue"), media_type or "all",
        ))

        old_total = 0
        new_total = 0
        diffs = []
        for session_id, rows in rows_by_session.items():
            session = sessions_by_id.get(session_id)
            if session is None:
                continue
            item = items_by_id.get(session["mediaItemId"])
            if item is None:
                continue
            if media_type and item.get("type") != media_type:
                continue

            old_value = old_total_for_session(rows, start, end)
            new_value = new_total_for_session(rows, start, end)
            old_total += old_value
            new_total += new_value
            if old_value != new_value:
                diffs.append((item.get("title"), session_id, old_value, new_value, rows))

        print("  old total: %d" % old_total)
        print("  new total: %d" % new_total)
        print("  difference: %+d" % (new_total - old_total))

        if diffs:
            print("  sessions that changed:")
            for title, session_id, old_value, new_value, rows in sorted(
                diffs, key=lambda d: abs(d[3] - d[2]), reverse=True
            ):
                print("    %-40s session %-6s %6d -> %6d  (%+d)" % (
                    (title or "?")[:40], session_id, old_value, new_value, new_value - old_value,
                ))
                for r in sorted(rows, key=lambda r: (r["loggedAtEpochDay"], r["createdAtEpochMillis"], r["id"])):
                    print("        id=%-5s value=%-6s %s known=%-5s counts=%s" % (
                        r["id"], r["progressValue"], epoch_day_to_date(r["loggedAtEpochDay"]),
                        r["hasKnownDate"], r["countsTowardObjectives"],
                    ))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1])
