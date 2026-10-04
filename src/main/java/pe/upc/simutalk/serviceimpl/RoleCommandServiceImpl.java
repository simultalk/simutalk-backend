package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.SeedRolesCommand;
import pe.upc.simutalk.entities.Role;
import pe.upc.simutalk.enums.Roles;
import pe.upc.simutalk.repositories.RoleRepository;
import pe.upc.simutalk.services.RoleCommandService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleCommandServiceImpl implements RoleCommandService {

    private final RoleRepository roleRepository;

    /** Idempotent: only inserts the roles that are still missing. */
    @Override
    public void handle(SeedRolesCommand command) {
        Arrays.stream(Roles.values())
                .filter(name -> !roleRepository.existsByName(name))
                .forEach(name -> roleRepository.save(new Role(name)));
    }
}
