package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PeriodManagementService implements PeriodManagementUseCase {

    private final PeriodRepository periodRepository;

    public PeriodManagementService(PeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    @Override
    public Period createPeriod(LocalDate periodDate, String comments) {
        // Validar que no exista un periodo para esa fecha
        periodRepository.findByPeriodDate(periodDate).ifPresent(p -> {
            throw new IllegalArgumentException("Ya existe un periodo para la fecha indicada");
        });

        Period period = new Period();
        period.setPeriodDate(periodDate);
        period.setComments(comments);
        period.setState(Period.State.OPEN);
        period.setCreatedAt(LocalDateTime.now());
        return periodRepository.save(period);
    }

    @Override
    public void closePeriod(Long periodId, String username) {
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));

        if (period.getState() != Period.State.OPEN) {
            throw new IllegalStateException("Solo se puede cerrar un periodo abierto");
        }

        // Validar que no haya inventario o marbetes pendientes
        if (periodRepository.hasInventoryOrLabels(periodId)) {
            throw new IllegalStateException("No se puede cerrar el periodo: hay inventario o marbetes pendientes");
        }

        period.setState(Period.State.CLOSED);
        period.setClosedBy(username);
        period.setClosedAt(LocalDateTime.now());
        periodRepository.update(period);
    }

    @Override
    public void lockPeriod(Long periodId, String username) {
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));

        if (period.getState() != Period.State.CLOSED) {
            throw new IllegalStateException("Solo se puede bloquear un periodo cerrado");
        }

        period.setState(Period.State.LOCKED);
        period.setLockedBy(username);
        period.setLockedAt(LocalDateTime.now());
        periodRepository.update(period);
    }
}
