package uk.gov.hmcts.reform.unspec.service.docmosis;

public enum DocmosisTemplates {
    N1("CV-UNS-GOR-ENG-0001.docx", "sealed_claim_form_%s");

    private final String template;
    private final String documentTitle;

    DocmosisTemplates(String template, String documentTitle) {
        this.template = template;
        this.documentTitle = documentTitle;
    }

    public String getTemplate() {
        return template;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }
}
