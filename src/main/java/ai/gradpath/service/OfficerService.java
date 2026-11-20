package ai.gradpath.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.gradpath.domain.CareerOfficer;
import ai.gradpath.repository.UserRepository;
import ai.gradpath.validation.Validators;

/** Career officer accounts. */
@Service
public class OfficerService {

    private final UserRepository users;

    public OfficerService(UserRepository users) {
        this.users = users;
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional(readOnly = true)
    public CareerOfficer require(Integer id) {
        if (users.findById(id).orElse(null) instanceof CareerOfficer o) {
            return o;
        }
        throw new NotFoundException("Career officer account not found.");
    }

    @PreAuthorize("hasRole('OFFICER')")
    @Transactional
    public Result<CareerOfficer> updateProfile(Integer officerId, String name, String office) {
        List<String> errors = new ArrayList<>();
        String e1 = Validators.checkName(name);
        String e2 = Validators.checkText(office, "Office / organisation name", 3, 120);
        if (e1 != null) {
            errors.add(e1);
        }
        if (e2 != null) {
            errors.add(e2);
        }
        if (!errors.isEmpty()) {
            return Result.fail(errors);
        }
        CareerOfficer o = require(officerId);
        o.setName(Validators.clean(name));
        o.setOffice(Validators.clean(office));
        return Result.ok(o);
    }
}
