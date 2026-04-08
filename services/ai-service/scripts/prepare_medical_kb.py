import argparse
import csv
import hashlib
import html
import json
import os
import re
from pathlib import Path


def clean_text(text):
    if text is None:
        return ""
    if isinstance(text, list):
        text = " ".join(str(item) for item in text if item is not None)
    text = html.unescape(str(text))
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def infer_topic(text):
    lowered = clean_text(text).lower()
    keyword_map = {
        "ophthalmology": ["glaucoma", "eye", "vision", "mat"],
        "respiratory": ["sinus", "asthma", "cough", "ho", "xoang"],
        "cardiology": ["heart", "blood pressure", "tim", "huyet ap"],
        "endocrinology": ["diabetes", "thyroid", "duong huyet"],
        "gastroenterology": ["stomach", "liver", "gan", "da day"],
        "neurology": ["headache", "seizure", "dau dau", "co giat"],
    }
    for topic, keywords in keyword_map.items():
        if any(keyword in lowered for keyword in keywords):
            return topic
    return "general"


def infer_risk_level(*values):
    text = " ".join(clean_text(value).lower() for value in values if value)
    high = ["pregnan", "mang thai", "newborn", "tre so sinh", "overdose", "qua lieu", "chest pain", "kho tho"]
    medium = ["child", "tre em", "elderly", "nguoi gia", "allergy", "di ung", "kidney", "liver", "benh nen"]
    if any(token in text for token in high):
        return "high"
    if any(token in text for token in medium):
        return "medium"
    return "low"


def stable_id(prefix, *parts):
    payload = "||".join(clean_text(part) for part in parts if part)
    digest = hashlib.sha1(payload.encode("utf-8")).hexdigest()[:12]
    return f"{prefix}_{digest}"


def iter_records(path):
    lower = path.lower()
    if lower.endswith(".jsonl"):
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    record = json.loads(line)
                except json.JSONDecodeError:
                    continue
                if isinstance(record, dict):
                    yield record
        return
    if lower.endswith(".csv"):
        with open(path, "r", encoding="utf-8", errors="ignore", newline="") as f:
            for row in csv.DictReader(f):
                if isinstance(row, dict):
                    yield row
        return
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        payload = json.load(f)
    if isinstance(payload, list):
        for item in payload:
            if isinstance(item, dict):
                yield item
        return
    if isinstance(payload, dict):
        for key in ("results", "data", "items"):
            if isinstance(payload.get(key), list):
                for item in payload[key]:
                    if isinstance(item, dict):
                        yield item
                return
        yield payload


def normalize_medquad(record):
    question = clean_text(record.get("question") or record.get("Question"))
    answer = clean_text(record.get("answer") or record.get("Answer"))
    title = clean_text(record.get("title") or record.get("focus") or question)
    if len(question) < 8 or len(answer) < 20:
        return None
    topic = clean_text(record.get("topic")) or infer_topic(f"{title} {question} {answer}")
    return {
        "id": clean_text(record.get("id")) or stable_id("medquad", title, question),
        "source": "MedQuAD",
        "type": "faq",
        "title": title,
        "question": question,
        "answer": answer,
        "topic": topic,
        "audience": "patient",
        "risk_level": infer_risk_level(question, answer),
        "needs_human_review": False,
        "chunk_text": f"Cau hoi: {question}\nTra loi: {answer}",
    }


def normalize_healthcaremagic(record):
    user_message = clean_text(
        record.get("user_message") or record.get("question") or record.get("query") or record.get("input")
    )
    doctor_reply = clean_text(
        record.get("doctor_reply") or record.get("answer") or record.get("response") or record.get("output")
    )
    if len(user_message) < 8 or len(doctor_reply) < 20:
        return None
    topic = clean_text(record.get("topic")) or infer_topic(f"{user_message} {doctor_reply}")
    return {
        "id": clean_text(record.get("id")) or stable_id("healthcaremagic", user_message, doctor_reply),
        "source": "HealthCareMagic",
        "type": "dialogue",
        "title": clean_text(record.get("title")) or user_message[:120],
        "user_message": user_message,
        "doctor_reply": doctor_reply,
        "topic": topic,
        "audience": "patient",
        "risk_level": infer_risk_level(user_message, doctor_reply),
        "needs_human_review": True,
        "chunk_text": f"Benh nhan hoi: {user_message}\nBac si tra loi: {doctor_reply}",
    }


def normalize_pubmedqa(record):
    question = clean_text(record.get("question") or record.get("query"))
    context = clean_text(record.get("abstract") or record.get("context") or record.get("abstract_summary"))
    final_decision = clean_text(record.get("final_decision") or record.get("decision") or record.get("label"))
    if len(question) < 8 or len(context) < 20:
        return None
    topic = clean_text(record.get("topic")) or infer_topic(f"{question} {context}")
    return {
        "id": clean_text(record.get("id")) or stable_id("pubmedqa", question, final_decision),
        "source": "PubMedQA",
        "type": "evidence",
        "title": clean_text(record.get("title")) or question,
        "question": question,
        "context": context,
        "final_decision": final_decision,
        "topic": topic,
        "audience": "clinician",
        "risk_level": infer_risk_level(question, context),
        "needs_human_review": True,
        "warning": "Thong tin nghien cuu khong thay the tu van ca nhan hoa.",
        "chunk_text": f"Cau hoi nghien cuu: {question}\nTom tat: {context}\nKet luan: {final_decision}",
    }


NORMALIZERS = {
    "medquad": normalize_medquad,
    "healthcaremagic": normalize_healthcaremagic,
    "meddialog": normalize_healthcaremagic,
    "pubmedqa": normalize_pubmedqa,
}


def dedupe_records(records):
    seen = set()
    for record in records:
        fingerprint = hashlib.sha1(record["chunk_text"].encode("utf-8")).hexdigest()
        if fingerprint in seen:
            continue
        seen.add(fingerprint)
        yield record


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-type", required=True, choices=sorted(NORMALIZERS.keys()))
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    normalizer = NORMALIZERS[args.source_type]
    records = []
    for raw in iter_records(args.input):
        normalized = normalizer(raw)
        if normalized:
            records.append(normalized)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8") as f:
        for record in dedupe_records(records):
            f.write(json.dumps(record, ensure_ascii=False) + "\n")

    print(
        json.dumps(
            {
                "status": "ok",
                "sourceType": args.source_type,
                "input": os.path.abspath(args.input),
                "output": str(output_path.resolve()),
                "recordCount": len(list(dedupe_records(records))),
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
