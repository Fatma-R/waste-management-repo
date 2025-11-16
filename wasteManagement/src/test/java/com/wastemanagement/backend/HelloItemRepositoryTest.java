package com.wastemanagement.backend;

import com.wastemanagement.backend.model.HelloItem;
import com.wastemanagement.backend.repository.HelloItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class HelloItemRepositoryTest {

    @Autowired
    private HelloItemRepository repository;

    @Test
    void saveAndLoadHelloItem() {
        // clean collection
        repository.deleteAll();

        // save
        HelloItem saved = repository.save(new HelloItem("Hello Mongo"));

        // reload
        var all = repository.findAll();

        assertThat(all).hasSize(1);
        HelloItem item = all.get(0);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getMessage()).isEqualTo("Hello Mongo");
    }
}