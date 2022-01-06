package tn.spring.security.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import tn.spring.security.entities.Role;

@Transactional
@Repository
public interface IRoleRepository extends JpaRepository<Role, Integer> {
	public Role findRoleByroleType(String user) throws Exception;
	public Role findRoleByidRole(int user) throws Exception;

}