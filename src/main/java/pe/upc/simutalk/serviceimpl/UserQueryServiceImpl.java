package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.GetAllUsersQuery;
import pe.upc.simutalk.dtos.GetUserByIdQuery;
import pe.upc.simutalk.dtos.GetUserByUsernameQuery;
import pe.upc.simutalk.entities.User;
import pe.upc.simutalk.repositories.UserRepository;
import pe.upc.simutalk.services.UserQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Optional<User> handle(GetUserByUsernameQuery query) {
        return userRepository.findByUsername(query.username());
    }
}
