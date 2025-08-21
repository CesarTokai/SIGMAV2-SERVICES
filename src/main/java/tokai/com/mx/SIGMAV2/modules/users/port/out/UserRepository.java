
package tokai.com.mx.SIGMAV2.modules.users.port.out;


import java.util.Optional;

import org.springframework.stereotype.Repository;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;


@Repository
public interface UserRepository {

    BeanUser save(BeanUser user);

    Object clone() throws CloneNotSupportedException;

    @Override
    boolean equals(Object obj);

    void finalize() throws Throwable;

    @Override
    int hashCode();

    @Override
    String toString();

    Optional<BeanUser> findByEmail(String email);
    

    boolean existsByEmail(String email);


    
}
