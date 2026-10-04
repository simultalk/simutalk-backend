package pe.upc.simutalk.repositories;

import pe.upc.simutalk.entities.CompanyProfile;
import pe.upc.simutalk.entities.Ruc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByRuc(Ruc ruc);
}
