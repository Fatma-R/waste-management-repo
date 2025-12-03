package com.wastemanagement.backend.repository.user;

import com.wastemanagement.backend.model.user.Admin;
import com.wastemanagement.backend.model.user.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends CrudRepository<Admin, String> {
    Optional<Admin> findByUser(User user);
}
