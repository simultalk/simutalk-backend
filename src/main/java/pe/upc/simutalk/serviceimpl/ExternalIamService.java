package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.services.IamContextFacade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Anti-corruption layer from profiles to iam. Goes through the IamContextFacade
 * contract published in shared; never touches iam classes.
 */
@Service
@RequiredArgsConstructor
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    public boolean existsUser(Long userId) {
        return iamContextFacade.existsUserById(userId);
    }
}
