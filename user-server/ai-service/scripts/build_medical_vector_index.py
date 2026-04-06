import argparse
import hashlib
import json
import math
import re
from pathlib import Path

import numpy as np


def clean_text(text):
    if text is None:
        return ""
    text = str(text).lower()
    text = re.sub(r"\s+", " ", text).strip()
    return text


def fold(text):
    import unicodedata

    normalized = unicodedata.normalize("NFD", clean_text(text))
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    normalized = normalized.replace("đ", "d").replace("Đ", "D")
    normalized = re.sub(r"[^a-z0-9\s]", " ", normalized)
    normalized = re.sub(r"\s+", " ", normalized).strip()
    return normalized


def tokenize(text):
    text = fold(text)
    words = [token for token in text.split() if len(token) >= 2]
    bigrams = [f"{words[i]}_{words[i + 1]}" for i in range(len(words) - 1)]
    return words + bigrams


def hashed_embedding(text, dim):
    vector = np.zeros(dim, dtype=np.float32)
    for token in tokenize(text):
        digest = hashlib.md5(token.encode("utf-8")).hexdigest()
        bucket = int(digest[:8], 16) % dim
        sign = 1.0 if int(digest[8:10], 16) % 2 == 0 else -1.0
        vector[bucket] += sign
    norm = np.linalg.norm(vector)
    if norm > 0:
        vector /= norm
    return vector


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--chunks", required=True)
    parser.add_argument("--output-prefix", required=True)
    parser.add_argument("--dim", type=int, default=768)
    args = parser.parse_args()

    chunk_path = Path(args.chunks)
    output_prefix = Path(args.output_prefix)
    output_prefix.parent.mkdir(parents=True, exist_ok=True)

    rows = []
    vectors = []
    with chunk_path.open("r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError:
                continue
            text = record.get("text") or ""
            rows.append(record)
            vectors.append(hashed_embedding(text, args.dim))

    matrix = np.vstack(vectors) if vectors else np.zeros((0, args.dim), dtype=np.float32)
    np.save(output_prefix.with_suffix(".npy"), matrix)
    with output_prefix.with_suffix(".jsonl").open("w", encoding="utf-8") as out:
        for row in rows:
            out.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(json.dumps({
        "status": "ok",
        "vectorFile": str(output_prefix.with_suffix(".npy").resolve()),
        "metaFile": str(output_prefix.with_suffix(".jsonl").resolve()),
        "vectorCount": int(matrix.shape[0]),
        "dimension": args.dim,
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
