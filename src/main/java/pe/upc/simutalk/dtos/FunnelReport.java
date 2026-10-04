package pe.upc.simutalk.dtos;

import java.util.Map;

/**
 * @param stages every pipeline stage (RECEIVED, INTERVIEWING, ASSESSED, SHORTLISTED, HIRED, REJECTED) with its count
 */
public record FunnelReport(Long jobPostingId, Map<String, Long> stages, long totalApplications) {
}
