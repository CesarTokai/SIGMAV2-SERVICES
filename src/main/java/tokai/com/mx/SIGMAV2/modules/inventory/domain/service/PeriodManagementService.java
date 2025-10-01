package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period.PeriodState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import java.time.LocalDate;

public class PeriodManagementService implements PeriodManagementUseCase {

    private final PeriodRepository periodRepository;

    public PeriodManagementService(PeriodRepository periodRepository) {
        this.periodRepository = periodRepository;
    }

    @Override
    public Period findById(Long id) {
        return periodRepository.findById(id).orElse(null);
    }

    @Override
    public Period createPeriod(LocalDate date, String comments, BeanUser user) {
        throw new UnsupportedOperationException("La creación de periodos solo está permitida para el administrador a través del servicio principal");
    }

    @Override
    public Page<Period> findAll(Pageable pageable) {
        return periodRepository.findAll(pageable);
    }

    @Override
    public Period updateComments(Long id, String comments) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
        period.setComments(comments);
        return periodRepository.save(period);
    }

    @Override
    public void deletePeriod(Long id) {
        periodRepository.deleteById(id);
    }



    @Override
    public Period openPeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
        if (period.getState() != PeriodState.DRAFT) {
            throw new IllegalStateException("Solo se puede abrir un periodo en estado draft o cerrado");
        }
        period.setState(PeriodState.OPEN);
        return periodRepository.save(period);
    }

    @Override
    public Period closePeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
        if (period.getState() != PeriodState.OPEN) {
            throw new IllegalStateException("Solo se puede cerrar un periodo abierto");
        }
        if (periodRepository.hasDependencies(id)) {
            throw new IllegalStateException("No se puede cerrar el periodo: tiene dependencias");
        }
        period.setState(PeriodState.CLOSED);
        return periodRepository.save(period);
    }

    @Override
    public Period lockPeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
        if (period.getState() != PeriodState.CLOSED) {
            throw new IllegalStateException("Solo se puede bloquear un periodo cerrado");
        }
        period.setState(PeriodState.LOCKED);
        return periodRepository.save(period);
    }

    @Override
    public boolean hasDependencies(Long id) {
        return periodRepository.hasDependencies(id);
    }
}
