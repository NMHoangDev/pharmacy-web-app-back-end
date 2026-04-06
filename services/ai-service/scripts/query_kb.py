import argparse
import csv
import hashlib
import html
import json
import os
import re
import sqlite3
import sys
from heapq import nlargest
from pathlib import Path

import numpy as np

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="ignore")


def clean_text(text):
    if text is None:
        return ""
    if isinstance(text, list):
        text = " ".join(str(item) for item in text if item is not None)
    text = html.unescape(str(text))
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def fold(text):
    if not text:
        return ""
    import unicodedata

    normalized = unicodedata.normalize("NFD", clean_text(text))
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    normalized = normalized.replace("đ", "d").replace("Đ", "D")
    normalized = re.sub(r"[^a-zA-Z0-9\s]", " ", normalized.lower())
    normalized = re.sub(r"\s+", " ", normalized).strip()
    return normalized


STOP_WORDS = {
    "thuoc", "la", "gi", "co", "tac", "dung", "hay", "tu", "van", "di", "cho",
    "toi", "minh", "ban", "nay", "khong", "nao", "cua", "voi", "bi", "benh",
    "how", "what", "when", "where", "which", "why", "the", "and", "for", "from", "vi",
}

ALIASES = {
    "paracetamol": ["acetaminophen", "apap"],
    "acetaminophen": ["paracetamol", "apap"],
    "amotoxin": ["amoxicillin", "amoxicilin", "amoxycillin"],
    "amoxicillin": ["amotoxin", "amoxicilin", "amoxycillin"],
    "amoxicilin": ["amoxicillin", "amotoxin"],
    "glucose": ["duong huyet", "blood sugar"],
    "hypertension": ["high blood pressure", "tang huyet ap"],
    "viem xoang": ["sinusitis", "sinus infection"],
    "xoang": ["sinusitis", "sinus"],
    "dau dau": ["headache", "migraine"],
    "dau": ["pain"],
    "ho khan": ["dry cough"],
    "ho": ["cough"],
    "ho co dom": ["productive cough"],
    "dau hong": ["sore throat"],
    "hong": ["throat"],
    "kho tho": ["shortness of breath", "breathless"],
    "sot": ["fever"],
    "dau da day": ["stomach pain", "gastritis"],
    "tieu duong": ["diabetes"],
    "cao huyet ap": ["hypertension", "high blood pressure"],
    "huyet ap cao": ["hypertension", "high blood pressure"],
    "di ung": ["allergy", "allergic reaction"],
    "mat ngu": ["insomnia"],
    "chong mat": ["dizziness", "vertigo"],
    "buon non": ["nausea"],
    "tieu chay": ["diarrhea"],
    "tao bon": ["constipation"],
    "nhiem trung tieu": ["urinary tract infection", "uti"],
    "viem hong": ["pharyngitis", "sore throat"],
    "cam cum": ["influenza", "flu"],
    "viem phe quan": ["bronchitis"],
    "viem phoi": ["pneumonia"],
}

SOURCE_PRIORITY = {
    "medquad": 30,
    "pubmedqa": 20,
    "healthcaremagic": 10,
    "meddialog": 10,
}


def tokenize(text):
    return [token for token in fold(text).split() if len(token) >= 2 and token not in STOP_WORDS]


def expand_queries(query):
    base = fold(query)
    expanded = []
    seen = set()

    def add(value):
        if value and value not in seen:
            seen.add(value)
            expanded.append(value)

    add(base)
    compact = " ".join(tokenize(query))
    if compact:
        add(compact)
    for phrase, aliases in ALIASES.items():
        folded_phrase = fold(phrase)
        if folded_phrase and folded_phrase in base:
            for alias in aliases:
                add(base.replace(folded_phrase, fold(alias)))
                add(fold(alias))
    for token in list(tokenize(query)):
        for alias in ALIASES.get(token, []):
            add(base.replace(token, fold(alias)))
            add(fold(alias))
    return expanded


def build_search_tokens(query):
    ordered = []
    seen = set()

    def add_token(token):
        if token and token not in seen:
            seen.add(token)
            ordered.append(token)

    folded_query = fold(query)
    for phrase, aliases in ALIASES.items():
        folded_phrase = fold(phrase)
        if folded_phrase and folded_phrase in folded_query:
            for alias in aliases:
                for token in tokenize(alias):
                    add_token(token)

    for token in tokenize(query):
        add_token(token)

    for variant in expand_queries(query):
        for token in tokenize(variant):
            add_token(token)

    return ordered


def hashed_embedding(text, dim):
    vector = np.zeros(dim, dtype=np.float32)
    for token in tokenize(text) + [f"{a}_{b}" for a, b in zip(tokenize(text), tokenize(text)[1:])]:
        digest = hashlib.md5(token.encode("utf-8")).hexdigest()
        bucket = int(digest[:8], 16) % dim
        sign = 1.0 if int(digest[8:10], 16) % 2 == 0 else -1.0
        vector[bucket] += sign
    norm = np.linalg.norm(vector)
    if norm > 0:
        vector /= norm
    return vector


def first_non_blank(*values):
    for value in values:
        cleaned = clean_text(value)
        if cleaned:
            return cleaned
    return ""


def normalize_record(record, source_name=None):
    source = clean_text(record.get("source") or source_name or "unknown")
    record_type = clean_text(record.get("type"))
    if not record_type:
        if record.get("doctor_reply") or record.get("user_message"):
            record_type = "dialogue"
        elif record.get("final_decision") or record.get("abstract") or record.get("context"):
            record_type = "evidence"
        else:
            record_type = "faq"

    title = first_non_blank(
        record.get("title"),
        record.get("question"),
        record.get("user_message"),
        record.get("query"),
    ) or "Khong ro tieu de"
    question = first_non_blank(record.get("question"), record.get("user_message"), record.get("query"))
    answer = first_non_blank(record.get("answer"), record.get("doctor_reply"), record.get("response"))
    context = first_non_blank(record.get("context"), record.get("abstract"), record.get("abstract_summary"))
    decision = first_non_blank(record.get("final_decision"), record.get("decision"), record.get("label"))
    topic = clean_text(record.get("topic")) or infer_topic(title, question, answer, context)
    audience = clean_text(record.get("audience")) or ("patient" if record_type != "evidence" else "clinician")
    risk_level = clean_text(record.get("risk_level")) or infer_risk_level(question, answer, context, decision)
    warning = clean_text(record.get("warning"))
    if not warning and risk_level in {"medium", "high"}:
        warning = "Can doi chieu them voi bac si hoac duoc si trong tinh huong ca nhan hoa."
    generic_name = first_non_blank(record.get("generic_name"), record.get("genericName"))
    snippet = build_snippet(
        {
            "type": record_type,
            "question": question,
            "answer": answer,
            "context": context,
            "decision": decision,
            "doctor_reply": record.get("doctor_reply"),
            "user_message": record.get("user_message"),
        }
    )
    searchable = " ".join(
        part for part in [title, question, answer, context, decision, topic, generic_name, source] if part
    )
    return {
        "title": title,
        "genericName": generic_name,
        "snippet": snippet,
        "source": source,
        "type": record_type,
        "topic": topic,
        "audience": audience,
        "riskLevel": risk_level or "low",
        "warning": warning,
        "searchable": searchable,
    }


def build_snippet(record):
    record_type = clean_text(record.get("type"))
    if record_type == "dialogue":
        user_message = clean_text(record.get("user_message") or record.get("question"))
        doctor_reply = clean_text(record.get("doctor_reply") or record.get("answer"))
        snippet = f"Benh nhan hoi: {user_message} Bac si tra loi: {doctor_reply}"
    elif record_type == "evidence":
        question = clean_text(record.get("question"))
        context = clean_text(record.get("context"))
        decision = clean_text(record.get("decision"))
        snippet = f"Cau hoi nghien cuu: {question} Tom tat bang chung: {context} Ket luan: {decision}"
    else:
        question = clean_text(record.get("question"))
        answer = clean_text(record.get("answer"))
        snippet = f"Cau hoi: {question} Tra loi: {answer}"
    return snippet[:900]


def infer_topic(*values):
    text = fold(" ".join(clean_text(value) for value in values if value))
    topic_keywords = {
        "ophthalmology": ["glaucoma", "mat", "vision", "eye"],
        "respiratory": ["ho", "cough", "xoang", "sinus", "asthma", "lung"],
        "cardiology": ["tim", "heart", "huyet ap", "blood pressure"],
        "endocrinology": ["diabetes", "duong huyet", "thyroid"],
        "gastroenterology": ["stomach", "da day", "ruot", "gan", "liver"],
        "neurology": ["dau dau", "headache", "co giat", "seizure"],
        "general": [],
    }
    for topic, keywords in topic_keywords.items():
        if any(keyword in text for keyword in keywords):
            return topic
    return "general"


def infer_risk_level(*values):
    text = fold(" ".join(clean_text(value) for value in values if value))
    high_markers = [
        "pregnan", "mang thai", "tre so sinh", "newborn", "overdose", "qua lieu",
        "chest pain", "dau nguc", "shortness of breath", "kho tho", "stroke", "dot quy",
    ]
    medium_markers = [
        "child", "tre em", "elderly", "nguoi gia", "chronic disease", "benh nen",
        "allergy", "di ung", "kidney", "suy than", "liver", "suy gan",
    ]
    if any(marker in text for marker in high_markers):
        return "high"
    if any(marker in text for marker in medium_markers):
        return "medium"
    return "low"


def score_record(normalized, query_variants, tokens):
    haystack = fold(normalized["searchable"])
    title_fold = fold(normalized["title"])
    generic_fold = fold(normalized["genericName"])
    score = 0
    for query in query_variants:
        if query and (query in title_fold or query in generic_fold):
            score += 24
        elif query and query in haystack:
            score += 12
    for token in tokens:
        if token in title_fold:
            score += 10
        if token in generic_fold:
            score += 8
        if token in haystack:
            score += 4
    if normalized["type"] == "faq":
        score += 3
    if normalized["type"] == "evidence" and any(token in {"study", "evidence", "nghien", "research"} for token in tokens):
        score += 6
    score += source_priority_bonus(normalized["source"])
    if normalized["riskLevel"] == "low":
        score += 1
    elif normalized["riskLevel"] == "high":
        score -= 2
    return score


def source_priority_bonus(source):
    folded_source = fold(source)
    for name, bonus in SOURCE_PRIORITY.items():
        if name in folded_source:
            return bonus
    return 0


def levenshtein(left, right):
    if left == right:
        return 0
    if not left:
        return len(right)
    if not right:
        return len(left)
    prev = list(range(len(right) + 1))
    for i, cl in enumerate(left, start=1):
        curr = [i]
        for j, cr in enumerate(right, start=1):
            cost = 0 if cl == cr else 1
            curr.append(min(curr[-1] + 1, prev[j] + 1, prev[j - 1] + cost))
        prev = curr
    return prev[-1]


def chroma_hits(query, db_path, collection_name, top_k):
    import chromadb
    from langchain_ollama import OllamaEmbeddings

    client = chromadb.PersistentClient(path=db_path)
    collection = client.get_collection(collection_name)
    embedder = OllamaEmbeddings(model="nomic-embed-text")
    qvec = embedder.embed_query(query)
    result = collection.query(
        query_embeddings=[qvec],
        n_results=top_k,
        include=["documents", "metadatas", "distances"],
    )
    documents = (result.get("documents") or [[]])[0]
    metadatas = (result.get("metadatas") or [[]])[0]
    distances = (result.get("distances") or [[]])[0]
    hits = []
    for idx, doc in enumerate(documents):
        meta = metadatas[idx] or {}
        hits.append(
            {
                "title": meta.get("title") or meta.get("brand_name") or meta.get("generic_name") or "Khong ro ten",
                "genericName": meta.get("generic_name") or meta.get("genericName") or "",
                "snippet": clean_text(doc)[:900],
                "source": meta.get("source") or "chroma",
                "type": meta.get("type") or "knowledge",
                "topic": meta.get("topic") or "",
                "audience": meta.get("audience") or "patient",
                "riskLevel": meta.get("risk_level") or meta.get("riskLevel") or "low",
                "warning": meta.get("warning") or "",
                "score": max(1, int(100 - ((distances[idx] if idx < len(distances) else 0) * 100))),
            }
        )
    return hits


def sqlite_hits(query, data_dir, top_k):
    db_file = os.path.join(data_dir, "medical_search.db")
    if not os.path.exists(db_file):
        return []

    query_variants = expand_queries(query)
    tokens = build_search_tokens(query)
    query_mode = detect_query_mode(query)
    file_groups = list_candidate_files(data_dir, query_mode)
    source_filters = []
    for names in file_groups:
        names = [os.path.splitext(name)[0] for name in names if name]
        if names:
            source_filters.append(names)
    match_query = " OR ".join(tokens[:8])
    if not match_query:
        return []

    db_uri = Path(db_file).resolve().as_uri() + "?mode=ro"
    conn = sqlite3.connect(db_uri, uri=True)
    conn.row_factory = sqlite3.Row
    try:
        for source_names in source_filters:
            where_clause = " OR ".join(["r.search_file = ?"] * len(source_names))
            sql = f"""
                SELECT
                    r.title,
                    r.generic_name,
                    r.snippet,
                    r.source,
                    r.type,
                    r.topic,
                    r.audience,
                    r.risk_level,
                    r.warning,
                    r.search_file,
                    bm25(medical_fts) AS rank_score
                FROM medical_fts
                JOIN records r ON r.rowid = medical_fts.rowid
                WHERE medical_fts MATCH ?
                  AND ({where_clause})
                LIMIT 60
            """
            hits = []
            for row in conn.execute(sql, [match_query, *source_names]):
                normalized = {
                    "title": row["title"],
                    "genericName": row["generic_name"],
                    "snippet": row["snippet"],
                    "source": row["source"],
                    "type": row["type"],
                    "topic": row["topic"],
                    "audience": row["audience"],
                    "riskLevel": row["risk_level"],
                    "warning": row["warning"],
                    "searchable": " ".join(
                        value for value in [
                            row["title"], row["generic_name"], row["snippet"], row["topic"], row["source"]
                        ] if value
                    ),
                }
                score = score_record(normalized, query_variants, tokens)
                score += max(0, int(30 - (row["rank_score"] or 0)))
                hits.append(
                    {
                        "title": row["title"],
                        "genericName": row["generic_name"] or "",
                        "snippet": row["snippet"],
                        "source": row["source"],
                        "type": row["type"],
                        "topic": row["topic"],
                        "audience": row["audience"],
                        "riskLevel": row["risk_level"],
                        "warning": row["warning"],
                        "score": score,
                    }
                )
            best = nlargest(top_k, hits, key=lambda item: item["score"])
            if best:
                return best
    finally:
        conn.close()
    return []


def vector_hits(query, data_dir, top_k):
    vector_file = Path(data_dir) / "medical_vectors.npy"
    meta_file = Path(data_dir) / "medical_vectors.jsonl"
    if not vector_file.exists() or not meta_file.exists():
        return []

    matrix = np.load(vector_file, mmap_mode="r")
    if matrix.shape[0] == 0:
        return []

    query_vector = hashed_embedding(" ".join(build_search_tokens(query)), int(matrix.shape[1]))
    scores = matrix @ query_vector
    top_n = min(max(top_k * 8, 24), int(matrix.shape[0]))
    indices = np.argpartition(scores, -top_n)[-top_n:]
    indices = indices[np.argsort(scores[indices])[::-1]]

    metadata = []
    with meta_file.open("r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            line = line.strip()
            if line:
                metadata.append(json.loads(line))

    hits = []
    for idx in indices[:top_n]:
        record = metadata[int(idx)]
        source = record.get("source") or "medical"
        hits.append(
            {
                "title": record.get("title") or "Không rõ tiêu đề",
                "genericName": "",
                "snippet": clean_text(record.get("text"))[:900],
                "source": source,
                "type": record.get("type") or "knowledge",
                "topic": record.get("topic") or "",
                "audience": record.get("audience") or "patient",
                "riskLevel": record.get("risk_level") or "low",
                "warning": record.get("warning") or "",
                "score": int(max(1, round(float(scores[int(idx)]) * 100))) + source_priority_bonus(source),
            }
        )
    return nlargest(top_k, hits, key=lambda item: item["score"])


def hybrid_hits(query, data_dir, top_k):
    sqlite_results = sqlite_hits(query, data_dir, top_k)
    vector_results = vector_hits(query, data_dir, top_k)
    merged = {}
    for hit in sqlite_results + vector_results:
        key = (hit.get("title"), hit.get("source"), hit.get("type"))
        existing = merged.get(key)
        if existing is None or hit["score"] > existing["score"]:
            merged[key] = hit
    if not merged:
        return []
    return nlargest(top_k, merged.values(), key=lambda item: item["score"])


def iter_json(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        obj = json.load(f)
    if isinstance(obj, list):
        for item in obj:
            if isinstance(item, dict):
                yield item
        return
    if isinstance(obj, dict):
        for key in ("results", "data", "items"):
            values = obj.get(key)
            if isinstance(values, list):
                for item in values:
                    if isinstance(item, dict):
                        yield item
                return
        yield obj


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


def iter_csv_rows(path):
    with open(path, "r", encoding="utf-8", errors="ignore", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if isinstance(row, dict):
                yield row


def iter_records(path):
    lower = path.lower()
    if lower.endswith(".jsonl"):
        yield from iter_jsonl(path)
        return
    if lower.endswith(".csv"):
        yield from iter_csv_rows(path)
        return
    if lower.endswith(".json"):
        yield from iter_json(path)
        return


def detect_query_mode(query):
    folded_query = fold(query)
    token_set = set(tokenize(query))
    research_markers = ["nghien cuu", "evidence", "study", "abstract", "pubmed", "bang chung"]
    dialogue_phrases = ["cam thay", "co sao khong", "nen lam gi", "phai lam sao", "trieu chung", "dau dau", "buon non", "chong mat", "dau bung"]
    dialogue_tokens = {"toi", "em", "minh", "bi", "ho", "sot"}
    if any(marker in folded_query for marker in research_markers):
        return "research"
    if any(marker in folded_query for marker in dialogue_phrases):
        return "dialogue"
    if token_set.intersection(dialogue_tokens):
        return "dialogue"
    return "faq"


def list_candidate_files(data_dir, query_mode):
    names = [name for name in sorted(os.listdir(data_dir)) if name.endswith(".jsonl") or name.endswith(".json") or name.endswith(".csv")]
    medquad_files = [name for name in names if "medquad" in fold(name)]
    healthcaremagic_files = [name for name in names if "healthcaremagic" in fold(name) or "meddialog" in fold(name)]
    other_files = [name for name in names if name not in medquad_files and name not in healthcaremagic_files]

    if query_mode == "faq":
        return [medquad_files, healthcaremagic_files, other_files]
    if query_mode == "dialogue":
        return [medquad_files + healthcaremagic_files, healthcaremagic_files, medquad_files, other_files]
    return [other_files + medquad_files, medquad_files, healthcaremagic_files]


def lexical_hits(query, data_dir, top_k):
    query_variants = expand_queries(query)
    tokens = tokenize(query)
    expanded_tokens = set(tokens)
    for token in list(tokens):
        expanded_tokens.update(fold(alias) for alias in ALIASES.get(token, []))

    query_mode = detect_query_mode(query)
    file_groups = list_candidate_files(data_dir, query_mode)
    for names in file_groups:
        if not names:
            continue
        scored = []
        for name in names:
            path = os.path.join(data_dir, name)
            source_name = os.path.splitext(name)[0]
            try:
                for record in iter_records(path):
                    normalized = normalize_record(record, source_name=source_name)
                    score = score_record(normalized, query_variants, list(expanded_tokens))
                    if score <= 0:
                        title_fold = fold(normalized["title"])
                        for query_variant in query_variants:
                            if query_variant and levenshtein(title_fold[: len(query_variant)], query_variant) <= 2:
                                score = 6
                                break
                    if score <= 0:
                        continue
                    hit = {
                        "title": normalized["title"],
                        "genericName": normalized["genericName"],
                        "snippet": normalized["snippet"],
                        "source": normalized["source"],
                        "type": normalized["type"],
                        "topic": normalized["topic"],
                        "audience": normalized["audience"],
                        "riskLevel": normalized["riskLevel"],
                        "warning": normalized["warning"],
                        "score": score,
                    }
                    scored.append(hit)
            except Exception:
                continue
        best = nlargest(top_k, scored, key=lambda item: item["score"])
        if best:
            strongest = best[0]["score"]
            if len(best) >= top_k or strongest >= 40 or query_mode == "faq":
                return best
    return []


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--query", required=True)
    parser.add_argument("--db-path", required=True)
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--collection", default="medical_kb")
    parser.add_argument("--top-k", type=int, default=5)
    args = parser.parse_args()

    response = {"hits": [], "backend": "none"}
    try:
        hits = hybrid_hits(args.query, args.data_dir, args.top_k)
        if hits:
            response["hits"] = hits
            response["backend"] = "hybrid"
            print(json.dumps(response, ensure_ascii=True))
            return
    except Exception as ex:
        response["hybridError"] = str(ex)

    if args.collection != "medical_kb":
        try:
            hits = chroma_hits(args.query, args.db_path, args.collection, args.top_k)
            if hits:
                response["hits"] = hits
                response["backend"] = "chroma"
                print(json.dumps(response, ensure_ascii=True))
                return
        except Exception as ex:
            response["chromaError"] = str(ex)

    try:
        hits = lexical_hits(args.query, args.data_dir, args.top_k)
        response["hits"] = hits
        response["backend"] = "data"
    except Exception as ex:
        response["dataError"] = str(ex)

    print(json.dumps(response, ensure_ascii=True))


if __name__ == "__main__":
    main()
