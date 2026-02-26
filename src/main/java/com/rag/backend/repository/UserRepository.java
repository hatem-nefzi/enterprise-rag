package com.rag.backend.repository;
import com.rag.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public
interface UserRepository extends JpaRepository<User, Long> {
     /**
     * Find user by email (used for authentication).
     *@param email User email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user with given email exists.
     * More efficient than findByEmail when you only need existence check.
     * @param email User email
     * @return true if user exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find user by role.
     * Useful for admin operations.
     * @param role User role (e.g., "ADMIN", "USER")
     * @return List of users with that role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    java.util.List<User> findByRole(String role);
}
