package pe.upc.simutalk.mappers;

import pe.upc.simutalk.dtos.EvidenceResource;
import pe.upc.simutalk.entities.Evidence;

public class EvidenceResourceFromEntityAssembler {

    public static EvidenceResource toResourceFromEntity(Evidence entity) {
        return new EvidenceResource(entity.getId(), entity.getAnswerId(), entity.getExcerpt(), entity.getStartOffset(),
                entity.getEndOffset());
    }
}
