error id: file://<WORKSPACE>/src/test/java/com/rag/backend/DatabaseTest.java:
file://<WORKSPACE>/src/test/java/com/rag/backend/DatabaseTest.java
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 89
uri: file://<WORKSPACE>/src/test/java/com/rag/backend/DatabaseTest.java
text:
```scala
package com.rag.backend;

import com.rag.backend.repository.DocumentRepository;
imporimpo@@rt com.rag.backend.model.Document;
import com.rag.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DatabaseTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 