package pe.upc.simutalk.repositories;

import pe.upc.simutalk.enums.ApplicationStatus;

/** JPQL projection: number of applications in one pipeline stage. */
public interface ApplicationStatusCount {

    ApplicationStatus getStatus();

    long getTotal();
}
