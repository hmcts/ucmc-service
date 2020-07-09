package uk.gov.hmcts.reform.ucmc.service.docmosis;

public enum DocmosisTemplates {
    OCCN1("CV-CMC-GOR-ENG-0001.docx", "sealed_claim_form_%s.pdf");

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
