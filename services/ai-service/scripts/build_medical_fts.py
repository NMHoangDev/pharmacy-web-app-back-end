import argparse
import json
import os
import sqlite3
from pathlib import Path


def iter_jsonl(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                item = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(item, dict):
                yield item


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--output", default=None)
    args = parser.parse_args()

    data_dir = Path(args.data_dir)
    output = Path(args.output) if args.output else data_dir / "medical_search.db"
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists():
        output.unlink()

    conn = sqlite3.connect(output)
    try:
        conn.executescript(
            """
            PRAGMA journal_mode=DELETE;
            CREATE TABLE records (
                id TEXT PRIMARY KEY,
                title TEXT,
                generic_name TEXT,
                snippet TEXT,
                source TEXT,
                type TEXT,
                topic TEXT,
                audience TEXT,
                risk_level TEXT,
                warning TEXT,
                searchable TEXT,
                search_file TEXT
            );
            CREATE VIRTUAL TABLE medical_fts USING fts5(
                title,
                generic_name,
                snippet,
                searchable,
                search_file,
                tokenize='unicode61'
            );
            """
        )

        count = 0
        for path in sorted(data_dir.glob("*.jsonl")):
            search_file = path.stem
            for record in iter_jsonl(path):
                title = record.get("title") or record.get("question") or record.get("user_message") or "Khong ro tieu de"
                generic_name = record.get("generic_name") or record.get("genericName") or ""
                snippet = record.get("chunk_text") or record.get("answer") or record.get("doctor_reply") or ""
                searchable = " ".join(
                    str(value) for value in [
                        title,
                        record.get("question"),
                        record.get("answer"),
                        record.get("user_message"),
                        record.get("doctor_reply"),
                        record.get("context"),
                        record.get("final_decision"),
                        record.get("topic"),
                        record.get("source"),
                    ] if value
                )
                record_id = f"{search_file}:{count}"
                conn.execute(
                    """
                    INSERT INTO records (
                        id, title, generic_name, snippet, source, type, topic, audience,
                        risk_level, warning, searchable, search_file
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        record_id,
                        title,
                        generic_name,
                        snippet,
                        record.get("source") or search_file,
                        record.get("type") or "knowledge",
                        record.get("topic") or "",
                        record.get("audience") or "patient",
                        record.get("risk_level") or "low",
                        record.get("warning") or "",
                        searchable,
                        search_file,
                    ),
                )
                conn.execute(
                    """
                    INSERT INTO medical_fts (rowid, title, generic_name, snippet, searchable, search_file)
                    VALUES (last_insert_rowid(), ?, ?, ?, ?, ?)
                    """,
                    (title, generic_name, snippet, searchable, search_file),
                )
                count += 1
        conn.commit()
        print(json.dumps({"status": "ok", "output": str(output.resolve()), "recordCount": count}, ensure_ascii=False))
    finally:
        conn.close()


if __name__ == "__main__":
    main()
