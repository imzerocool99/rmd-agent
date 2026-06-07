package com.rmd.rag;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Lightweight RAG service using TF-IDF cosine similarity.
 * No external dependencies — runs entirely in-process.
 * Corpus is mutable at runtime via the Admin API.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int MAX_AUDIT = 50;

    private final List<KnowledgeDocument> corpus = new ArrayList<>(InstitutionalKnowledgeBase.ALL);
    private final List<Map<String, Double>> docVectors = new ArrayList<>();
    private final Map<String, Double> idfScores = new HashMap<>();

    // Ring buffer of recent retrieval events for the audit log
    private final Deque<Map<String, Object>> auditLog = new ArrayDeque<>();

    @PostConstruct
    public synchronized void buildIndex() {
        docVectors.clear();
        idfScores.clear();

        int n = corpus.size();
        Map<String, Integer> df = new HashMap<>();

        for (KnowledgeDocument doc : corpus) {
            new HashSet<>(tokenize(doc.getContent() + " " + doc.getTitle()))
                .forEach(term -> df.merge(term, 1, Integer::sum));
        }

        df.forEach((term, count) ->
            idfScores.put(term, Math.log((double)(n + 1) / (count + 1)) + 1.0)
        );

        for (KnowledgeDocument doc : corpus) {
            docVectors.add(buildVector(doc.getContent() + " " + doc.getTitle() + " " + doc.getCategory()));
        }

        log.info("RAG index built: {} documents, {} unique terms", n, df.size());
    }

    // ── RETRIEVAL ─────────────────────────────────────────────────────────────

    public List<KnowledgeDocument> retrieve(String query, int topK) {
        Map<String, Double> queryVec = buildVector(query);

        List<double[]> scores = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            scores.add(new double[]{ i, cosineSimilarity(queryVec, docVectors.get(i)) });
        }

        return scores.stream()
            .sorted((a, b) -> Double.compare(b[1], a[1]))
            .limit(topK)
            .filter(s -> s[1] > 0.01)
            .map(s -> corpus.get((int) s[0]))
            .collect(Collectors.toList());
    }

    public String retrieveAsContext(String query) {
        List<Map<String, Object>> scored = retrieveScored(query, 3);
        if (scored.isEmpty()) return "";

        appendAudit(query, scored);

        StringBuilder sb = new StringBuilder("Relevant institutional knowledge:\n");
        for (Map<String, Object> r : scored) {
            KnowledgeDocument doc = (KnowledgeDocument) r.get("doc");
            sb.append("\n[").append(doc.getTitle()).append("]\n")
              .append(doc.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    /** Returns documents with their similarity scores — used by RAG Tester in admin UI. */
    public List<Map<String, Object>> retrieveScored(String query, int topK) {
        Map<String, Double> queryVec = buildVector(query);

        List<double[]> scores = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            scores.add(new double[]{ i, cosineSimilarity(queryVec, docVectors.get(i)) });
        }

        return scores.stream()
            .sorted((a, b) -> Double.compare(b[1], a[1]))
            .limit(topK)
            .filter(s -> s[1] > 0.01)
            .map(s -> {
                KnowledgeDocument doc = corpus.get((int) s[0]);
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("doc", doc);
                r.put("id",       doc.getId());
                r.put("title",    doc.getTitle());
                r.put("category", doc.getCategory());
                r.put("score",    Math.round(s[1] * 1000.0) / 1000.0);
                r.put("preview",  doc.getContent().length() > 120
                    ? doc.getContent().substring(0, 120) + "..." : doc.getContent());
                return r;
            })
            .collect(Collectors.toList());
    }

    // ── ADMIN — DOCUMENT MANAGEMENT ───────────────────────────────────────────

    public synchronized List<KnowledgeDocument> getAllDocuments() {
        return Collections.unmodifiableList(corpus);
    }

    public synchronized void addDocument(KnowledgeDocument doc) {
        corpus.removeIf(d -> d.getId().equals(doc.getId())); // replace if same id
        corpus.add(doc);
        buildIndex();
        log.info("RAG: document added/updated — id={}", doc.getId());
    }

    public synchronized boolean updateDocument(String id, String title, String category, String content) {
        for (int i = 0; i < corpus.size(); i++) {
            if (corpus.get(i).getId().equals(id)) {
                corpus.set(i, new KnowledgeDocument(id, title, category, content));
                buildIndex();
                log.info("RAG: document updated — id={}", id);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeDocument(String id) {
        boolean removed = corpus.removeIf(d -> d.getId().equals(id));
        if (removed) {
            buildIndex();
            log.info("RAG: document removed — id={}", id);
        }
        return removed;
    }

    // ── ADMIN — STATS & AUDIT ─────────────────────────────────────────────────

    public Map<String, Object> getStats() {
        int termCount = idfScores.size();
        Map<String, Long> byCategory = corpus.stream()
            .collect(Collectors.groupingBy(KnowledgeDocument::getCategory, Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("documentCount", corpus.size());
        stats.put("uniqueTerms",   termCount);
        stats.put("byCategory",    byCategory);
        return stats;
    }

    public List<Map<String, Object>> getAuditLog() {
        synchronized (auditLog) {
            return new ArrayList<>(auditLog);
        }
    }

    // ── INTERNAL ──────────────────────────────────────────────────────────────

    private void appendAudit(String query, List<Map<String, Object>> results) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", java.time.Instant.now().toString());
        entry.put("query", query);
        entry.put("retrieved", results.stream()
            .map(r -> r.get("title") + " (" + r.get("score") + ")")
            .collect(Collectors.toList()));
        synchronized (auditLog) {
            auditLog.addFirst(entry);
            while (auditLog.size() > MAX_AUDIT) auditLog.removeLast();
        }
    }

    private Map<String, Double> buildVector(String text) {
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) return Collections.emptyMap();

        Map<String, Long> tf = tokens.stream()
            .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        Map<String, Double> vec = new HashMap<>();
        tf.forEach((term, count) -> {
            double idf = idfScores.getOrDefault(term, Math.log(2.0) + 1.0);
            vec.put(term, (count.doubleValue() / tokens.size()) * idf);
        });
        return vec;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double bVal = b.get(e.getKey());
            if (bVal != null) dot += e.getValue() * bVal;
            normA += e.getValue() * e.getValue();
        }
        for (double v : b.values()) normB += v * v;
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(
                text.toLowerCase()
                    .replaceAll("[^a-z0-9%$.]", " ")
                    .split("\\s+"))
            .filter(t -> t.length() > 2)
            .filter(t -> !STOPWORDS.contains(t))
            .collect(Collectors.toList());
    }

    private static final Set<String> STOPWORDS = Set.of(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "has",
        "her", "was", "one", "our", "out", "its", "that", "this", "with",
        "they", "from", "have", "been", "will", "when", "your", "which",
        "also", "each", "more", "than", "then", "into", "over", "such",
        "only", "both", "very", "well", "may", "any", "per", "via",
        "must", "upon", "most", "some", "other", "these", "those"
    );
}
