error id: file://<WORKSPACE>/src/main/java/com/rag/backend/repository/UserRepository.java:java/util/Optional#
file://<WORKSPACE>/src/main/java/com/rag/backend/repository/UserRepository.java
empty definition using pc, found symbol in pc: java/util/Optional#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 342
uri: file://<WORKSPACE>/src/main/java/com/rag/backend/repository/UserRepository.java
text:
```scala
package com.rag.backend.repository;
import com.rag.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
interface UserRepository extends JpaRepository<User, Long> {
    @@Optional<User> findById(Long id);
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: java/util/Optional#