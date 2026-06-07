package com.rmd.admin;

import com.rmd.rag.KnowledgeDocument;
import com.rmd.rag.RagService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final RagService ragService;
    private final LlmSettings settings;

    public AdminController(RagService ragService, LlmSettings settings) {
        this.ragService = ragService;
        this.settings   = settings;
    }

    // ── HEALTH ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("backend", "UP");
        result.put("ragIndex", ragService.getStats());
        result.put("llm", Map.of(
            "model",       settings.getModel(),
            "ollamaUrl",   settings.getOllamaUrl(),
            "ollamaStatus", pingOllama()
        ));
        return result;
    }

    private String pingOllama() {
        try {
            // Ollama root responds with a plain "Ollama is running" string
            String url = settings.getOllamaUrl().replace("/api/generate", "");
            new RestTemplate().getForObject(url, String.class);
            return "UP";
        } catch (Exception e) {
            return "DOWN — " + e.getMessage();
        }
    }

    // ── RAG DOCUMENTS ─────────────────────────────────────────────────────────

    @GetMapping("/rag/documents")
    public List<Map<String, Object>> listDocuments() {
        return ragService.getAllDocuments().stream()
            .map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",       d.getId());
                m.put("title",    d.getTitle());
                m.put("category", d.getCategory());
                m.put("content",  d.getContent());
                m.put("preview",  d.getContent().length() > 100
                    ? d.getContent().substring(0, 100) + "..." : d.getContent());
                return m;
            })
            .collect(Collectors.toList());
    }

    @PostMapping("/rag/documents")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody Map<String, String> body) {
        String id       = body.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8));
        String title    = body.getOrDefault("title", "Untitled");
        String category = body.getOrDefault("category", "products");
        String content  = body.getOrDefault("content", "");

        if (content.isBlank()) return ResponseEntity.badRequest()
            .body(Map.of("error", "content cannot be empty"));

        ragService.addDocument(new KnowledgeDocument(id, title, category, content));
        return ResponseEntity.ok(Map.of("status", "added", "id", id,
            "indexStats", ragService.getStats()));
    }

    @PutMapping("/rag/documents/{id}")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable String id, @RequestBody Map<String, String> body) {

        String title    = body.getOrDefault("title", "");
        String category = body.getOrDefault("category", "");
        String content  = body.getOrDefault("content", "");

        boolean updated = ragService.updateDocument(id, title, category, content);
        if (!updated) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(Map.of("status", "updated", "id", id,
            "indexStats", ragService.getStats()));
    }

    @DeleteMapping("/rag/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String id) {
        boolean removed = ragService.removeDocument(id);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("status", "deleted", "id", id,
            "indexStats", ragService.getStats()));
    }

    // ── PDF UPLOAD ────────────────────────────────────────────────────────────

    @PostMapping("/rag/upload-pdf")
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title",    required = false) String title,
            @RequestParam(value = "category", defaultValue = "custom") String category) {
        try {
            PDDocument pdf = Loader.loadPDF(file.getBytes());
            String text = new PDFTextStripper().getText(pdf);
            pdf.close();

            if (text.isBlank()) return ResponseEntity.badRequest()
                .body(Map.of("error", "PDF appears to have no extractable text (may be scanned image)"));

            String id       = UUID.randomUUID().toString().substring(0, 8);
            String docTitle = (title != null && !title.isBlank()) ? title : file.getOriginalFilename().replace(".pdf", "");
            ragService.addDocument(new KnowledgeDocument(id, docTitle, category, text.trim()));

            return ResponseEntity.ok(Map.of(
                "status", "added", "id", id, "title", docTitle,
                "chars", text.length(), "indexStats", ragService.getStats()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PDF SPLIT & IMPORT ───────────────────────────────────────────────────

    @PostMapping("/rag/upload-pdf-split")
    public ResponseEntity<Map<String, Object>> uploadPdfSplit(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "irs-rules") String category) {
        try {
            PDDocument pdf = Loader.loadPDF(file.getBytes());
            int totalPages = pdf.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            String baseTitle = file.getOriginalFilename().replace(".pdf", "");

            // Extract text per page
            List<String> pages = new ArrayList<>();
            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(stripper.getText(pdf));
            }
            pdf.close();

            // Detect section boundaries by heading patterns
            List<Map<String, Object>> sections = detectSections(pages, baseTitle);

            // Add each section as a separate document
            int added = 0;
            for (Map<String, Object> sec : sections) {
                String content = (String) sec.get("content");
                if (content.trim().length() < 100) continue; // skip tiny fragments
                String id    = UUID.randomUUID().toString().substring(0, 8);
                String title = (String) sec.get("title");
                ragService.addDocument(new KnowledgeDocument(id, title, category, content.trim()));
                added++;
            }

            return ResponseEntity.ok(Map.of(
                "status", "split-added",
                "sections", added,
                "pages", totalPages,
                "indexStats", ragService.getStats()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String cleanPageText(String page) {
        // Remove IRS print metadata lines and formatting artifacts
        java.util.regex.Pattern metaPat = java.util.regex.Pattern.compile(
            "(?m)^.*(Userid:|Schema:|Leadpct:|Fileid:|XSL/XML|Ok to Print|Init\\. & Date|" +
            "MUST be removed before printing|type and rule above prints|" +
            "departmental reproduction|Catalog Number|Jan \\d{2}, \\d{4}|" +
            "Page \\d+ of \\d+|Internal Revenue Service|www\\.irs\\.gov).*$"
        );
        String cleaned = metaPat.matcher(page).replaceAll("").trim();
        // Collapse excessive blank lines
        cleaned = cleaned.replaceAll("(?m)^\\s*\\n{2,}", "\n\n");
        return cleaned;
    }

    private boolean isFrontMatter(String cleanedPage) {
        // Skip pages that are mostly TOC entries (lots of dots "....") or very short
        long dotLines = cleanedPage.lines()
            .filter(l -> l.contains("......") || l.matches("^\\s*\\.{4,}.*"))
            .count();
        long totalLines = cleanedPage.lines().count();
        if (totalLines == 0) return true;
        // If >40% of lines are dot-leaders (TOC) or content < 150 chars, treat as front matter
        return cleanedPage.length() < 150 || (double) dotLines / totalLines > 0.4;
    }

    private List<Map<String, Object>> detectSections(List<String> pages, String baseTitle) {
        List<Map<String, Object>> sections = new ArrayList<>();
        java.util.regex.Pattern headingPat = java.util.regex.Pattern.compile(
            "^(Chapter\\s+\\d+|Part\\s+[IVXLCDM]+|Part\\s+\\d+|Section\\s+\\d+|[A-Z][A-Z\\s,\\-]{8,50})$",
            java.util.regex.Pattern.MULTILINE
        );

        String currentTitle   = "Introduction";
        StringBuilder current = new StringBuilder();
        boolean contentStarted = false;

        for (String rawPage : pages) {
            String page = cleanPageText(rawPage);

            // Skip front-matter pages until we hit real content
            if (!contentStarted) {
                if (isFrontMatter(page)) continue;
                contentStarted = true;
            }

            java.util.regex.Matcher m = headingPat.matcher(page);
            if (m.find()) {
                String heading = m.group().trim();
                if (current.length() > 200) {
                    Map<String, Object> sec = new LinkedHashMap<>();
                    sec.put("title",   baseTitle + " — " + currentTitle);
                    sec.put("content", current.toString().trim());
                    sections.add(sec);
                }
                currentTitle = heading;
                current      = new StringBuilder();
            }
            current.append(page).append("\n");

            // Flush oversized sections
            if (current.length() > 5000) {
                Map<String, Object> sec = new LinkedHashMap<>();
                sec.put("title",   baseTitle + " — " + currentTitle + " (cont.)");
                sec.put("content", current.toString().trim());
                sections.add(sec);
                current = new StringBuilder();
            }
        }

        // Last section
        if (current.length() > 200) {
            Map<String, Object> sec = new LinkedHashMap<>();
            sec.put("title",   baseTitle + " — " + currentTitle);
            sec.put("content", current.toString().trim());
            sections.add(sec);
        }

        // Fallback: chunk by 4000 chars if no headings detected
        if (sections.isEmpty()) {
            String fullText = pages.stream()
                .map(this::cleanPageText)
                .collect(java.util.stream.Collectors.joining("\n"));
            int chunkSize = 4000;
            for (int i = 0; i < fullText.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, fullText.length());
                Map<String, Object> sec = new LinkedHashMap<>();
                sec.put("title",   baseTitle + " — Section " + (i / chunkSize + 1));
                sec.put("content", fullText.substring(i, end));
                sections.add(sec);
            }
        }
        return sections;
    }

    // ── URL IMPORT ────────────────────────────────────────────────────────────

    @PostMapping("/rag/import-url")
    public ResponseEntity<Map<String, Object>> importUrl(@RequestBody Map<String, String> body) {
        String url      = body.getOrDefault("url", "").trim();
        String category = body.getOrDefault("category", "custom");

        if (url.isBlank()) return ResponseEntity.badRequest()
            .body(Map.of("error", "url cannot be empty"));

        try {
            org.jsoup.nodes.Document html = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; RMD-RAG-Importer/1.0)")
                .timeout(15000)
                .get();

            String title = html.title();
            String text  = html.body().text();

            if (text.isBlank()) return ResponseEntity.badRequest()
                .body(Map.of("error", "No extractable text content found at URL"));

            String id = UUID.randomUUID().toString().substring(0, 8);
            if (title.isBlank()) title = url;
            ragService.addDocument(new KnowledgeDocument(id, title, category, text.trim()));

            return ResponseEntity.ok(Map.of(
                "status", "added", "id", id, "title", title,
                "url", url, "chars", text.length(), "indexStats", ragService.getStats()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rag/rebuild")
    public Map<String, Object> rebuildIndex() {
        ragService.buildIndex();
        return Map.of("status", "rebuilt", "indexStats", ragService.getStats());
    }

    // ── RAG TESTER ────────────────────────────────────────────────────────────

    @PostMapping("/rag/test")
    public Map<String, Object> testQuery(@RequestBody Map<String, Object> body) {
        String query = String.valueOf(body.getOrDefault("query", ""));
        int topK = (int) body.getOrDefault("topK", 3);

        List<Map<String, Object>> results = ragService.retrieveScored(query, topK);

        String context = ragService.retrieveAsContext(query);

        return Map.of(
            "query",   query,
            "results", results,
            "augmentedPromptPreview",
                "You are a helpful IRA RMD financial assistant.\n\n" +
                context + "\n\nClient context: [client data here]\n\nAnswer: " + query
        );
    }

    // ── LLM SETTINGS ──────────────────────────────────────────────────────────

    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("model",      settings.getModel());
        s.put("temperature", settings.getTemperature());
        s.put("topP",       settings.getTopP());
        s.put("numPredict", settings.getNumPredict());
        s.put("ollamaUrl",  settings.getOllamaUrl());
        return s;
    }

    @PostMapping("/settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, Object> body) {
        if (body.containsKey("model"))
            settings.setModel(String.valueOf(body.get("model")));
        if (body.containsKey("temperature"))
            settings.setTemperature(Double.parseDouble(String.valueOf(body.get("temperature"))));
        if (body.containsKey("topP"))
            settings.setTopP(Double.parseDouble(String.valueOf(body.get("topP"))));
        if (body.containsKey("numPredict"))
            settings.setNumPredict(Integer.parseInt(String.valueOf(body.get("numPredict"))));

        return Map.of("status", "updated", "settings", getSettings());
    }

    // ── AUDIT LOG ─────────────────────────────────────────────────────────────

    @GetMapping("/audit")
    public List<Map<String, Object>> getAuditLog() {
        return ragService.getAuditLog();
    }
}
