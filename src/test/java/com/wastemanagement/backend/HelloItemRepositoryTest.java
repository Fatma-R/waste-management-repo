package com.wastemanagement.backend;

import com.wastemanagement.backend.model.HelloItem;
import com.wastemanagement.backend.repository.HelloItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test") // <-- makes Spring use application-test.properties
class HelloItemRepositoryTest {

    @Autowired
    private HelloItemRepository repository;

    @Test
    void saveAndLoadHelloItem() {
        // Create a unique test message
        String testMessage = "Hello Mongo " + UUID.randomUUID();

        // Save only this test item
        HelloItem saved = repository.save(new HelloItem(testMessage));

        // Reload the item by its ID
        Optional<HelloItem> found = repository.findById(saved.getId());
        assertThat(found).isPresent();

        HelloItem item = found.get();
        assertThat(item.getId()).isNotNull();
        assertThat(item.getMessage()).isEqualTo(testMessage);

        // Clean up only this test item
        repository.deleteById(saved.getId());
    }
}
