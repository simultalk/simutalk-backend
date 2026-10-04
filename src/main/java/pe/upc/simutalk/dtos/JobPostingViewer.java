package pe.upc.simutalk.dtos;

/**
 * Who is reading job postings, as far as visibility rules care.
 *
 * @param companyId    company of the viewer, or {@code null} if the viewer has none
 * @param unrestricted {@code true} for administrators and internal lookups: sees everything
 */
public record JobPostingViewer(Long companyId, boolean unrestricted) {

    public static JobPostingViewer unrestrictedViewer() {
        return new JobPostingViewer(null, true);
    }

    public static JobPostingViewer ofCompany(Long companyId) {
        return new JobPostingViewer(companyId, false);
    }
}
