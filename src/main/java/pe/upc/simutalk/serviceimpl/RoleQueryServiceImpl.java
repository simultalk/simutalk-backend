package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetAllRolesQuery;
import pe.upc.simutalk.dtos.GetRoleByNameQuery;
import pe.upc.simutalk.entities.Role;
import pe.upc.simutalk.repositories.RoleRepository;
import pe.upc.simutalk.services.RoleQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoleQueryServiceImpl implements RoleQueryService {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> handle(GetAllRolesQuery query) {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> handle(GetRoleByNameQuery query) {
        return roleRepository.findByName(query.name());
    }
}
