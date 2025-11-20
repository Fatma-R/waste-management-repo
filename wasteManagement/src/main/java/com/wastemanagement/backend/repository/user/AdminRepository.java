package com.wastemanagement.backend.repository.user;

import com.wastemanagement.backend.model.user.Admin;
import org.springframework.data.repository.CrudRepository;

public interface AdminRepository extends CrudRepository<Admin, String> {
}
