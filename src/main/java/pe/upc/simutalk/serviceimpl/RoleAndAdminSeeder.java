package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.SeedAdminUserCommand;
import pe.upc.simutalk.dtos.SeedRolesCommand;
import pe.upc.simutalk.services.RoleCommandService;
import pe.upc.simutalk.services.UserCommandService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * Seeds the role catalog on startup and, when ADMIN_USERNAME and ADMIN_PASSWORD are
 * set, the initial administrator (public sign-up cannot create admins).
 */
@Slf4j
@Service
public class RoleAndAdminSeeder {

    private final RoleCommandService roleCommandService;
    private final UserCommandService userCommandService;
    private final String adminUsername;
    private final String adminPassword;

    public RoleAndAdminSeeder(RoleCommandService roleCommandService,
                                        UserCommandService userCommandService,
                                        @Value("${authorization.bootstrap-admin.username:}") String adminUsername,
                                        @Value("${authorization.bootstrap-admin.password:}") String adminPassword) {
        this.roleCommandService = roleCommandService;
        this.userCommandService = userCommandService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    /** Runs first: other contexts' startup handlers may need the roles to exist. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationReadyEvent event) {
        roleCommandService.handle(new SeedRolesCommand());
        log.info("Roles seeding verified");

        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_USERNAME/ADMIN_PASSWORD not set; no bootstrap admin");
            return;
        }
        userCommandService.handle(new SeedAdminUserCommand(adminUsername, adminPassword));
    }
}
