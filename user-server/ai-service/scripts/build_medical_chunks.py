import argparse
import json
import math
import re
from pathlib import Path


def clean_text(text):
    if text is None:
        return ""
    text = str(text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def word_chunks(text, chunk_words=140, overlap_words=30):
    words = clean_text(text).split()
    if not words:
        return []
    if len(words) <= chunk_words:
        return [" ".join(words)]
    chunks = []
    step = max(1, chunk_words - overlap_words)
    for start in range(0, len(words), step):
        chunk = words[start:start + chunk_words]
        if not chunk:
            continue
        chunks.append(" ".join(chunk))
        if start + chunk_words >= len(words):
            break
    return chunks


def build_source_text(record):
    source = clean_text(record.get("source"))
    record_type = clean_text(record.get("type"))
    if source == "MedQuAD" or record_type == "faq":
        question = clean_text(record.get("question"))
        answer = clean_text(record.get("answer") or record.get("chunk_text"))
        title = clean_text(record.get("title") or question)
        return f"Title: {title}\nQuestion: {question}\nAnswer: {answer}"
    user_message = clean_text(record.get("user_message"))
    doctor_reply = clean_text(record.get("doctor_reply"))
    title = clean_text(record.get("title") or user_message[:120])
    return f"Title: {title}\nPatient: {user_message}\nDoctor: {doctor_reply}"


def chunk_record(record, chunk_words, overlap_words):
    source_text = build_source_text(record)
    pieces = word_chunks(source_text, chunk_words=chunk_words, overlap_words=overlap_words)
    chunks = []
    for index, piece in enumerate(pieces):
        chunks.append(
            {
                "chunk_id": f"{record.get('id', 'record')}_chunk_{index + 1}",
                "record_id": record.get("id"),
                "source": record.get("source"),
                "type": record.get("type"),
                "topic": record.get("topic"),
                "audience": record.get("audience"),
                "risk_level": record.get("risk_level"),
                "warning": record.get("warning", ""),
                "title": record.get("title"),
                "text": piece,
                "chunk_index": index,
                "chunk_count": len(pieces),
            }
        )
    return chunks


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--chunk-words", type=int, default=140)
    parser.add_argument("--overlap-words", type=int, default=30)
    args = parser.parse_args()

    input_dir = Path(args.input_dir)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    count = 0
    with output_path.open("w", encoding="utf-8") as out:
        for path in sorted(input_dir.glob("*.jsonl")):
            if path.name == output_path.name:
                continue
            for line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
                line = line.strip()
                if not line:
                    continue
                try:
                    record = json.loads(line)
                except json.JSONDecodeError:
                    continue
                for chunk in chunk_record(record, args.chunk_words, args.overlap_words):
                    out.write(json.dumps(chunk, ensure_ascii=False) + "\n")
                    count += 1

    print(json.dumps({
        "status": "ok",
        "output": str(output_path.resolve()),
        "chunkCount": count,
        "chunkWords": args.chunk_words,
        "overlapWords": args.overlap_words,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
